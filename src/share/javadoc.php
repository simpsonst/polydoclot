<?php // -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * "webzip.php" - PHP script to serve simple static content
 * Copyright (C) 2019  Steven Simpson
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Contact Steven Simpson <https://github.com/simpsonst>
 */

mb_internal_encoding("UTF-8");

if (!function_exists("str_ends_with")) {
    function str_ends_with($haystack, $needle) {
        return substr_compare($haystack, $needle, -strlen($needle)) == 0;
    }
}

if (!function_exists("str_starts_with")) {
    function str_starts_with($haystack, $needle) {
        return substr_compare($haystack, $needle, 0, strlen($needle)) == 0;
    }
}

function safe_header($header) {
    #    printf("Missed header: %s\n", $header);
    header($header);
}

function location_header($path, $query = null) {
    if (isset($_SERVER['WEBZIP_PREFIX']))
        $path = $_SERVER['WEBZIP_PREFIX'].$path;
    if ($query !== null)
        $path .= '?'.$query;
    safe_header("Location: ".$path);
}

function xml_decl($type = 'text/xml') {
    safe_header("Content-Type: ".$type."; charset=UTF-8");
    print "<?xml version='1.0' encoding='UTF-8' ?>\n";
}

function link_template_path($setting) {
    $path = isset($_SERVER['WEBZIP_XSLTPFX']) ?
          $_SERVER['WEBZIP_XSLTPFX'] : '/';
    $path .= $setting;
    if (isset($_SERVER['WEBZIP_XSLTSFX']))
        $path .= $_SERVER['WEBZIP_XSLTSFX'];
    else
        $path .= '.xsl';
    return $path;
}

function link_template($setting) {
    $path = link_template_path($setting);
    print "<?xml-stylesheet type='text/xsl' href='";
    print htmlentities($path, ENT_XML1);
    print "' ?>\n";
}

global $elem_stack, $char_offset;
$elem_stack = array();
$char_offset = 0;

/* Close the start tag if necessary. */
function ensure_close(&$top, &$stack_elem_res, &$lastindent) {
    global $elem_stack, $char_offset;

    $top = count($elem_stack);
    if ($top == 0) {
        $lastindent = '';
        return FALSE;
    }

    $stack_elem_res = $elem_stack[$top - 1];
    $stack_elem = &$elem_stack[$top - 1];
    $lastindent = $stack_elem['indent'];

    if (isset($stack_elem['attrs'])) {
        unset($stack_elem['attrs']);
        print ">";
        $char_offset += 1;
        if (isset($stack_elem['preformat'])) {
            //print "\n";
            //$char_offset = 0;
        } else if (!isset($stack_elem['inline'])) {
            print "\n$lastindent";
            $char_offset = mb_strlen($lastindent);
        }
    }

    return TRUE;
}

function start_elem($name, $flags='') {
    global $elem_stack, $char_offset;

    ensure_close($top, $stack_elem, $lastindent);

    $attrindent = "$lastindent".str_repeat(' ', 2 + strlen($name));
    $stack_elem = array('name' => $name,
                        'indent' => "$lastindent  ",
                        'attrs' => $attrindent);
    if (strpos($flags, 'I') !== FALSE ||
        ($top > 0 && isset($elem_stack[$top - 1]['inline'])))
        $stack_elem['inline'] = TRUE;
    if (strpos($flags, 'P') !== FALSE ||
        ($top > 0 && isset($elem_stack[$top - 1]['preformat'])))
        $stack_elem['preformat'] = TRUE;
    if ($char_offset > strlen($lastindent) &&
        !isset($stack_elem['inline']) && !isset($stack_elem['preformat'])) {
        print "\n$lastindent";
        $char_offset = mb_strlen($lastindent);
    }
    print "<$name";
    $char_offset += mb_strlen("<$name");
    array_push($elem_stack, $stack_elem);
    if (strpos($flags, 'N') !== FALSE)
        attr('xmlns', 'http://www.lancs.ac.uk/~simpsons/javadoc/2022');
    if (strpos($flags, 'P') !== FALSE)
        attr('xml:space', 'preserve');
}

function attr($name, $value=NULL) {
    global $elem_stack, $char_offset;

    $top = count($elem_stack);
    if (!isset($elem_stack[$top - 1]['attrs'])) return;
    if (is_null($value)) $value = $name;
    $line = "$name=\"".htmlentities($value,
                                    ENT_COMPAT | ENT_XML1, "UTF-8")."\"";
    $len = mb_strlen($line);
    $attrindent = $elem_stack[$top - 1]['attrs'];
    if ($len + $char_offset > 75 && $char_offset > mb_strlen($attrindent)) {
        print "\n$attrindent";
        $char_offset = mb_strlen($attrindent);
    } else {
        print ' ';
        $char_offset += 1;
    }
    print $line;
    $char_offset += $len;
}

/* Provide a relative link to a URL. */
function attr_href($name, $value) {
    attr($name, relativize_url($value));
}

function relativize_url($tgt, $base = NULL) {
    if ($tgt == NULL) return NULL;
    if ($base == NULL)
        get_self($base, $dummy);

    $basecomp = parse_url($base);
    $tgtcomp = parse_url($tgt);
    if (!same_elem($basecomp, $tgtcomp, 'scheme')) return $tgt;
    if (!same_elem($basecomp, $tgtcomp, 'host')) return $tgt;
    if (!same_elem($basecomp, $tgtcomp, 'port')) return $tgt;
    if (!same_elem($basecomp, $tgtcomp, 'user')) return $tgt;
    if (!same_elem($basecomp, $tgtcomp, 'pass')) return $tgt;

    /* Split the base URL path into elements.  Empty the last element.
     * This simulates resolving "./" against the base to get a
     * possible common prefix. */
    $baseelems = explode('/', $basecomp['path']);
    $baselim = count($baseelems);
    $baseelems[$baselim - 1] = '';

    /* Split the target URL path into elements.  Reset and preserve
     * the last element. */
    $tgtelems = explode('/', $tgtcomp['path']);
    $tgtlim = count($tgtelems);
    $tgtleaf = $tgtelems[$tgtlim - 1];
    $tgtelems[$tgtlim - 1] = '';

    /* Find and remove common initial path elements. */
    $count = $baselim - 1;
    for ($i = 0; $count > 0 && $i < $baselim && $i < $tgtlim &&
            $baseelems[$i] == $tgtelems[$i]; $i++) {
        unset($tgtelems[$i]);
        $count--;
    }

    $result = str_repeat('../', $count);
    $result .= implode('/', $tgtelems);
    $result .= $tgtleaf;
    if ($result == '') $result = '.';
    if (isset($tgtcomp['query']))
        $result .= '?'.$tgtcomp['query'];
    if (isset($tgtcomp['fragment']))
        $result .= '#'.$tgtcomp['fragment'];

    // /* Also generate the absolute local version. */
    // $absresult = $tgtcomp['path'];
    // if (isset($tgtcomp['query']))
    //     $absresult .= '?'.$tgtcomp['query'];
    // if (isset($tgtcomp['fragment']))
    //     $absresult .= '#'.$tgtcomp['fragment'];
    // if (strlen($absresult) < strlen($result))
    //     return $absresult;

    /* Choose the shorter version. */
    return $result;
}

function same_elem($a, $b, $k) {
    if (isset($a[$k])) {
        if (isset($b[$k])) {
            return $a[$k] == $b[$k];
        } else {
            return FALSE;
        }
    } else if (isset($b[$k])) {
        return FALSE;
    } else {
        return TRUE;
    }
}

/* Provide a relative link to an internal URL. */
function attr_link($name, $value) {
    attr_href($name, $_SERVER['CONTEXT_PREFIX'].$value);
}

function attr_ref($name, &$value) {
    if (isset($value) && $value !== NULL)
        attr($name, $value);
}

function attr_boolref($name, &$value) {
    if (isset($value))
        attr_bool($name, $value);
}

function attr_bool($name, $value) {
    if ($value)
        attr($name, 'true');
}

function cdata($text) {
    global $char_offset;

    ensure_close($top, $stack_elem, $lastindent);

    if (!isset($stack_elem['preformat']) &&
        $char_offset == mb_strlen($lastindent)) {
        $text = preg_replace('/^\s+/', '', $text);
    }

    if (isset($stack_elem['preformat'])) {
        $lines = explode("\n", $text);
        for ($i = 0; $i < count($lines); $i += 1) {
            if ($i != 0) {
                print "\n";
                $char_offset = 0;
            }
            $line = htmlentities($lines[$i],
                                 ENT_NOQUOTES | ENT_XML1, "UTF-8");
            print $line;
            $char_offset += mb_strlen($line);
        }
    } else {
        $text = preg_replace('/\s+/', ' ', $text);
        $words = explode(' ', htmlentities($text,
                                           ENT_NOQUOTES | ENT_XML1, "UTF-8"));
        for ($i = 0; $i < count($words); $i += 1) {
            $line = $words[$i];
            $len = mb_strlen($line);
            if ($i != 0) {
                if ($len + $char_offset > 76) {
                    print "\n$lastindent";
                    $char_offset = mb_strlen($lastindent);
                } else {
                    print ' ';
                    $char_offset += 1;
                }
            }
            print $line;
            $char_offset += $len;
        }
    }
}

function end_elem() {
    global $elem_stack, $char_offset;

    $stack_elem = array_pop($elem_stack);
    $top = count($elem_stack);
    if ($top == 0) {
        $lastindent = '';
    } else {
        $lastindent = $elem_stack[$top - 1]['indent'];
    }
    if (isset($stack_elem['attrs'])) {
        $line = " />";
        print $line;
        $char_offset += mb_strlen($line);
    } else {
        if (!isset($stack_elem['inline']) && !isset($stack_elem['preformat'])) {
            print "\n$lastindent";
            $char_offset = mb_strlen($lastindent);
        }
        $line = "</".$stack_elem['name'].">";
        print $line;
        $char_offset += mb_strlen($line);
    }
}


function attr_deploy() {
    global $elem_stack;
    if (count($elem_stack) != 1) return;
    if (!isset($_SERVER['WEBZIP_XSLTCTXT'])) return;
    $loc = $_SERVER['WEBZIP_XSLTCTXT'];
    attr_href('deploy-base', $loc);
    #attr('href', CURRENT_URL);
}

function not_found() {
    http_response_code(404);
    xml_decl('text/xml');
    link_template('not-found');
    start_elem('not-found');
    attr_deploy();
    attr('request-uri', $_SERVER['REQUEST_URI']);
    if (isset($_SERVER['HTTP_REFERER']))
        attr('referrer-uri', $_SERVER['HTTP_REFERER']);
    end_elem();
}

function get_self(&$path, &$query) {
    $bits = parse_url($_SERVER['REQUEST_URI']);
    $path = $bits['path'];
    if (isset($bits['query']))
        $query = $bits['query'];
    else
        $query = null;
}






if (!preg_match('/^(HEAD|GET)$/', $_SERVER['REQUEST_METHOD'])) {
    http_response_code(405);
    safe_header('Allow: GET, HEAD');
    xml_decl('text/xml');
    link_template('bad-method');
    start_elem('bad-method');
    attr_deploy();
    attr('request-uri', $_SERVER['REQUEST_URI']);
    if (isset($_SERVER['HTTP_REFERER']))
        attr('referrer-uri', $_SERVER['HTTP_REFERER']);
    attr('method', $_SERVER['REQUEST_METHOD']);
    end_elem();
    exit;
}

# http_response_code(200);
# safe_header('Content-Type: text/plain; charset=UTF-8');
// print_r($_SERVER);
// exit;

$root = $_SERVER["WEBZIP_HOME"];
if (isset($_SERVER['PATH_INFO'])) {
    $subfile = $_SERVER['PATH_INFO'];
} else {
    $subfile = "";
}
$zipfile = $root;
while (!empty($subfile)) {
    #printf("sub=%s zip=%s\n", $subfile, $zipfile);
    preg_match('/^(\/[^\/]*)(.*)$/', $subfile, $matches);
    $zipfile .= $matches[1];
    $subfile = $matches[2];
    if (is_dir($zipfile)) continue;
    if (!file_exists("$zipfile.webzip")) {
        not_found();
        exit;
    }
    $zipfile = "$zipfile.webzip";
    break;
}
#printf("final sub=%s zip=%s\n", $subfile, $zipfile);
#exit;
/* Now $zipfile ends in ".webzip" if a zip was found, or something
 * else if a directory was found.  In the latter case, if $zipfile
 * doesn't end in "/", do a redirection to a path with the slash. */

if (empty($subfile)) {
    if (str_ends_with($zipfile, ".webzip")) {
        /* Redirect to the overview summary. */
        http_response_code(303);
        get_self($path, $query);
        location_header("$path/overview-summary", $query);
        exit;
    }

    if (str_ends_with($zipfile, "/")) {
        /* Scan directory entries. */
        $rawelems = scandir($zipfile);
        $elems = array();
        foreach ($rawelems as $elem) {
            if (str_starts_with($elem, ".")) continue;
            if (is_dir($zipfile.$elem)) {
                array_push($elems,
                           array('display' => $elem,
                                 'type' => 'dir',
                                 'vpath' => rawurlencode($elem).'/'));
                continue;
            }
            if (str_ends_with($elem, ".webzip")) {
                $elem = substr($elem, 0, -7);
                array_push($elems,
                           array('display' => $elem,
                                 'type' => 'zip',
                                 'vpath' => rawurlencode($elem)
                                 .'/overview-summary'));
                continue;
            }
        }

        /* Produce an XML directory listing with a configured XSLT
         * transformation. */
        get_self($path, $query);
        xml_decl('text/xml');
        link_template('dir-list');
        start_elem('dir-list');
        attr_deploy();
        attr('path', $path);
        foreach ($elems as $elem) {
            start_elem('elem');
            attr('type', $elem['type']);
            attr('name', $elem['display']);
            attr('href', $elem['vpath']);
            end_elem();
        }
        end_elem();
        exit;
    }

    /* Redirect by adding a slash to the virtual path. */
    http_response_code(303);
    get_self($path, $query);
    location_header("$path/", $query);
    exit;
}

if ($subfile == '/') {
    http_response_code(303);
    get_self($path, $query);
    location_header($path."overview-summary", $query);
    exit;
}

// $subfile = $_SERVER['PATH_TRANSLATED'];
// if (empty($root)) {
//     /* Find the zip file, and the path within it. */
//     $cwd = dirname($_SERVER['SCRIPT_FILENAME']).'/';
//     $cwdlen = strlen($cwd);
//     if (strncmp($cwd, $subfile, $cwdlen)) die("Path $subfile outside home");
//     $subfile = '/'.substr($subfile, $cwdlen);
//     $zipfile = '.';
//     while (!empty($subfile)) {
//         preg_match('/^(\/[^\/]*)(.*)$/', $subfile, $matches);
//         $zipfile .= $matches[1];
//         $subfile = $matches[2];
//         if (!file_exists($zipfile)) die("File disappeared");
//         if (!is_dir($zipfile)) break;
//     }
// } else {
//     $cwd = $_SERVER['DOCUMENT_ROOT'];
//     $cwdlen = strlen($cwd);
//     $subfile = '/'.substr($subfile, $cwdlen);
//     $zipfile = $root;
//     while (!empty($subfile)) {
//         preg_match('/^(\/[^\/]*)(.*)$/', $subfile, $matches);
//         $zipfile .= $matches[1];
//         $subfile = $matches[2];
//         if (is_dir($zipfile)) continue;
//         if (!file_exists("$zipfile.webzip")) {
//             http_response_code(404);
//             exit;
//         }
//         $zipfile = "$zipfile.webzip";
//         break;
//     }
// }

// header('Content-Type: text/plain; charset=UTF-8');
// printf("Trying %s in %s...\n", $subfile, $zipfile);
// var_dump($_SERVER);
// exit;

/* For a conditional GET, check whether the zip's date is strictly
 * newer than the one specified.  If not, indicate that a cached
 * version can be used. */
$ziptime = filemtime($zipfile);
// if (isset($_SERVER['HTTP_IF_MODIFIED_SINCE']) && 
//     strtotime($_SERVER['HTTP_IF_MODIFIED_SINCE']) >= $ziptime) {
//     http_response_code(304);
//     exit;
// }

function split_accept(&$map, $text, $func = "parse_mime_tok") {
    for ( ; ; ) {
        parse_mime_ws($text);
        if (!$func($text, $key)) return FALSE;
        $map[$key] = 1.0;
        while (parse_mime_wsparam($text, $name, $value)) {
            if ($name == 'q') {
                $map[$key] = floatval($value);
                break;
            }
        }
        parse_mime_ws($text);
        if (parse_mime_delim($text, ',')) continue;
        if (parse_mime_end($text)) return TRUE;
        return FALSE;
    }
}

function parse_mime_delim(&$text, $delim) {
    if (strlen($text) == 0) return FALSE;
    if ($text[0] != $delim) return FALSE;
    $text = substr($text, strlen($delim));
    return TRUE;
}

function parse_mime_ws(&$text, $min = 0) {
    if (!preg_match('/^([ \t]{'.$min.',})/', $text, $parts))
        return FALSE;
    $value = $parts[1];
    $text = substr($text, strlen($value));
    return TRUE;
}

function parse_mime_wsdelim(&$text, $delim, $min = 0) {
    $work = $text;
    if (!parse_mime_ws($work, $min))
        return FALSE;
    if (!parse_mime_delim($work, $delim))
        return FALSE;
    $text = $work;
    return TRUE;
}

function parse_mime_qs(&$text, &$value) {
    if (!preg_match('/^("([^\"\\\\]|(\\.))*")/', $text, $parts))
        return FALSE;
    $value = $parts[1];
    $text = substr($text, strlen($value));
    $value = substr($value, 1, -1);
    $value = preg_replace('/\\./', '$1', $value);
    return TRUE;
}

function parse_mime_tok(&$text, &$value) {
    if (!preg_match('/^([^][()<>@,;:\\\\"\/?={} \t]+)/', $text, $parts))
        return FALSE;
    $value = $parts[1];
    $text = substr($text, strlen($value));
    return TRUE;
}

function parse_mime_wsqs(&$text, &$value, $min = 0) {
    $work = $text;
    if (!parse_mime_ws($work, $min))
        return FALSE;
    if (!parse_mime_qs($work, $value))
        return FALSE;
    $text = $work;
    return TRUE;
}

function parse_mime_wstok(&$text, &$value, $min = 0) {
    $work = $text;
    if (!parse_mime_ws($work, $min))
        return FALSE;
    if (!parse_mime_tok($work, $value))
        return FALSE;
    $text = $work;
    return TRUE;
}

function parse_mime_qsortok(&$text, &$value) {
    return parse_mime_qs($text, $value) ||
        parse_mime_tok($text, $value);
}

function parse_mime_wsqsortok(&$text, &$value, $min = 0) {
    $work = $text;
    if (!parse_mime_ws($work, $min))
        return FALSE;
    if (!parse_mime_qsortok($work, $value))
        return FALSE;
    $text = $work;
    return TRUE;
}

function parse_mime_end($text) {
    return strlen($text) == 0;
}

function parse_mime_wsend(&$text, $min = 0) {
    $work = $text;
    if (parse_mime_ws($work, $min) &&
        parse_mime_end($work)) {
        $text = $work;
        return TRUE;
    }
    return FALSE;
}

function parse_mime_type(&$text, &$major, &$minor) {
    $work = $text;
    if (parse_mime_tok($work, $major) &&
        parse_mime_delim($work, '/') &&
        parse_mime_tok($work, $minor)) {
        $text = $work;
        return TRUE;
    }
    return FALSE;
}

function parse_mime_inttype(&$text, &$value) {
    if (parse_mime_type($text, $major, $minor)) {
        $value = "$major/$minor";
        return TRUE;
    }
    return FALSE;
}

function parse_mime_loosetype(&$text, &$major, &$minor) {
    $work = $text;
    if (parse_mime_type($work, $major, $minor)) {
        $text = $work;
        return TRUE;
    }
    if (parse_mime_delim($work, '*')) {
        $text = $work;
        $major = "*";
        $minor = "*";
        return TRUE;
    }
    return FALSE;
}

function parse_mime_intloosetype(&$text, &$value) {
    if (parse_mime_loosetype($text, $major, $minor)) {
        $value = "$major/$minor";
        return TRUE;
    }
    return FALSE;
}

function parse_mime_wstype(&$text, &$major, &$minor, $min = 0) {
    $work = $text;
    if (parse_mime_ws($work, $min) &&
        parse_mime_type($work, $major, $minor)) {
        $text = $work;
        return TRUE;
    }
    return FALSE;
}

function parse_mime_param(&$text, &$key, &$value) {
    $work = $text;
    if (parse_mime_delim($work, ';') &&
        parse_mime_wstok($work, $key) &&
        parse_mime_wsdelim($work, '=') &&
        parse_mime_wsqsortok($work, $value)) {
        $text = $work;
        return TRUE;
    }
    return FALSE;
}

function parse_mime_wsparam(&$text, &$key, &$value, $min = 0) {
    $work = $text;
    if (parse_mime_ws($work, $min) &&
        parse_mime_param($work, $key, $value)) {
        $text = $work;
        return TRUE;
    }
    return FALSE;
}

function parse_mime_typeparams(&$text, &$major, &$minor, &$map) {
    $work = $text;
    if (!parse_mime_type($work, $major, $minor)) return FALSE;
    while (parse_mime_wsparam($work, $key, $value))
        $map[$key] = $value;
    $text = $work;
    return TRUE;
}

function get_type_q($value, $qs) {
    global $accept_type;
    if (!preg_match('/^([^\/]*)\/(.*)$/', $value, $parts))
        return 0.0;
    $major = $parts[1];
    $minor = $parts[2];
    if (isset($accept_type["$major/$minor"]))
        return $qs * $accept_type["$major/$minor"];
    if (isset($accept_type["$major/*"]))
        return $qs * $accept_type["$major/*"];
    if (isset($accept_type["*/*"]))
        return $qs * $accept_type["*/*"];
    return 0.0;
}

function get_lang_q($value, $qs) {
    global $accept_lang;
    if (!preg_match('/^([^-]*)(-.*)?$/', $value, $parts))
        return 0.0;
    $major = $parts[1];
    $minor = $parts[2];
    if (isset($accept_lang["$major-$minor"]))
        return $qs * $accept_lang["$major-$minor"];
    if (isset($accept_lang["$major"]))
        return $qs * $accept_lang["$major"];
    if (isset($accept_lang["*"]))
        return $qs * $accept_lang["*"];
    return 0.0;
}

function get_charset_q($value, $qs) {
    global $accept_charset;
    if (isset($accept_charset[$value]))
        return $qs * $accept_charset[$value];
    if (isset($accept_charset["*"]))
        return $qs * $accept_charset["*"];
    return 0.0;
}

/* Start accessing the zipfile. */
$zip = new ZipArchive();
$zip->open($zipfile) || die("Failed to open zip");
try {
    /* Try to find a file beginning with the requested name.  If it is a
     * prefix, the next character must be a dot. */
    $cands = array();
    $ext = "$subfile.";
    $extlen = strlen($ext);
    #printf("Searching for [%s] and prefix [%s]...\n", $subfile, $ext);
    for ($i = 0; $i < $zip->numFiles; $i++) {
        $stat = $zip->statIndex($i);
        $path = '/'.$stat['name'];
        #printf("  Checking [%s]\n", $path);
        if ($subfile == $path || strncmp($path, $ext, $extlen) == 0)
            array_push($cands, $stat);
    }
    #var_dump($cands);
    if (count($cands) == 0) {
        not_found();
        exit;
    }
    ## 'size' and 'comp_size'

    /* Determine what forms of content negotiation the client accepts. */
    if (isset($_SERVER['HTTP_NEGOTIATE'])) {
        $negotiate = array_flip(preg_split('/\s*[,\s]+\s*/',
                                           $_SERVER['HTTP_NEGOTIATE']));
    } else {
        $negotiate = array();
    }
    #var_dump($negotiate);

    /* Read the content meta-data. */
    $meta = array();
    $meta_text = $zip->getFromName('content-types.tab');
    $meta_text = "DEFAULT: t=text/plain c=UTF-8\n$meta_text";
    foreach (preg_split("/((\r?\n)|(\r\n?))/", $meta_text) as $line) {
        $fp = $zip->getStream('content-types.tab') ||
            die("Failed to open meta-data");
        if (!parse_mime_wstok($line, $sfx)) continue;
        if (!parse_mime_wsdelim($line, ':')) continue;
        while (parse_mime_wstok($line, $key) &&
               parse_mime_wsdelim($line, '=')) {
            unset($pfx);
            switch ($key) {
            case 't':
                $pfx = 'type';
                if (parse_mime_wstype($line, $major, $minor))
                    $meta[$sfx][$pfx] = "$major/$minor";
                break;

            case 'c':
                $pfx = 'charset';
                if (parse_mime_wsqsortok($line, $value))
                    $meta[$sfx][$pfx] = $value;
                break;

            case 'l':
                $pfx = 'lang';
                if (parse_mime_wsqsortok($line, $value))
                    $meta[$sfx][$pfx] = $value;
                break;
            }
            if (!isset($pfx)) continue;
            $meta[$sfx][$pfx."_qs"] = 1.0;
            while (parse_mime_ws($line) &&
                   parse_mime_param($line, $key, $value))
                $meta[$sfx][$pfx."_$key"] = $value;
        }
    }
    #print "\nMetadata:\n";
    #var_dump($meta);

    if (!isset($negotiate['trans'])) {
        /* The client isn't negotiating, so we need to examine the
         * Accept-* headers. */
        global $accept_lang;
        $accept_lang = array();
        if (!split_accept($accept_lang,
                          isset($_SERVER['HTTP_ACCEPT_LANGUAGE']) ?
                          $_SERVER['HTTP_ACCEPT_LANGUAGE'] : '*'))
            $accept_lang = array("*" => 1);
        global $accept_charset;
        $accept_charset = array();
        if (!split_accept($accept_charset,
                          isset($_SERVER['HTTP_ACCEPT_CHARSET']) ?
                          $_SERVER['HTTP_ACCEPT_CHARSET'] : '*'))
            $accept_charset = array("*" => 1);
        global $accept_type;
        $accept_type = array();
        if (!split_accept($accept_type, isset($_SERVER['HTTP_ACCEPT']) ?
                          $_SERVER['HTTP_ACCEPT'] : '*/*',
                          "parse_mime_intloosetype"))
            $accept_type = array("*/*" => 1);
    }

    #error_log("HTTP_ACCEPT: ".$_SERVER['HTTP_ACCEPT']);
    #error_log("Accept-Language: ".print_r($accept_lang, TRUE));
    #error_log("Accept-Charset: ".print_r($accept_charset, TRUE));
    #error_log("Accept: ".print_r($accept_type, TRUE));
    #print "\nAccepted languages:\n";
    #var_dump($accept_lang);
    #print "\nAccepted charsets:\n";
    #var_dump($accept_charset);
    #print "\nAccepted types:\n";
    #var_dump($accept_type);

    #print "\nCandidates before:\n";
    #var_dump($cands);

    /* For each matching file, compute a quality value.  Keep track of
     * how many different languages, types and charsets are
     * supported. */
    $avail_types = array();
    $avail_charsets = array();
    $avail_langs = array();
    unset($bestmatch);
    $bestmatch_qs = 0.0;
    $expire = 40;
    foreach ($cands as $index => &$cand) {
        if ($expire-- <= 0) break;
        #printf("Working on %s...\n", $cand['name']);

        /* Assume the default content type and charset. */
        $type = $meta['DEFAULT']['type'];
        $type_qs = $meta['DEFAULT']['type_qs'];
        $charset = $meta['DEFAULT']['charset'];
        $charset_qs = $meta['DEFAULT']['charset_qs'];

        /* Languages are accumulated. */
        $langs = array();

        /* Iterate over the candidate's suffixes. */
        $rem = $cand['name'];
        $rem = preg_replace('/^[^.]*/', '', $rem);
        while (!empty($rem)) {
            if ($expire-- <= 0) break;
            #printf("  Remaining suffixes: [%s]\n", $rem);
            if (!preg_match('/^(\.[^.]*)(.*)$/', $rem, $parts)) break;
            $sfx = $parts[1];
            $rem = $parts[2];
            #printf("  Applying %s\n", $parts[1]);

            if (isset($meta[$sfx]['type'])) {
                $type = $meta[$sfx]['type'];
                $avail_types[$type] = TRUE;
                $type_qs = $meta[$sfx]['type_qs'];
            }

            if (isset($meta[$sfx]['charset'])) {
                $charset = $meta[$sfx]['charset'];
                $avail_charsets[$charset] = TRUE;
                $charset_qs = $meta[$sfx]['charset_qs'];
            }

            if (isset($meta[$sfx]['lang'])) {
                $l = $meta[$sfx]['lang'];
                $qs = $meta[$sfx]['lang_qs'];
                $avail_langs[$l] = TRUE;
                $langs[$l] = $qs;
            }
        }

        unset($bqs);
        foreach ($langs as $lang => $qs) {
            if (!isset($bqs) || $qs > $bqs)
                $bqs = $qs;
        }
        unset($qs);
        $cand['oqs'] = $type_qs * $charset_qs;
        if (isset($bqs)) $cand['oqs'] *= $bqs;

        /* If no languages are implied by suffixes, use the default. */
        if (count($langs) == 0) {
            $l = $meta['DEFAULT']['lang'];
            $qs = $meta['DEFAULT']['lang_qs'];
            $avail_langs[$l] = TRUE;
            $langs[$l] = $qs;
        }

        /* Multiply the qs values by the client's corresponding q
         * value. */
        $type_qs = get_type_q($type, $type_qs);
        $charset_qs = get_charset_q($charset, $charset_qs);
        foreach ($langs as $lang => &$qs)
            $qs = get_lang_q($lang, $qs);
        unset($qs);

        /* Get the best language. */
        $bestlang_qs = 0.0;
        unset($bestlang);
        foreach ($langs as $lang => $qs)
            if ($qs > $bestlang_qs) {
                $bestlang_qs = $qs;
                $bestlang = $lang;
            }

        /* Compute an over-all score. */
        $qs = $type_qs * $bestlang_qs * $charset_qs;

        /* Keep track of the best score. */
        #  || !isset($bestmatch)
        if ($qs > $bestmatch_qs) {
            $bestmatch_qs = $qs;
            $bestmatch = $index;
        }

        /* Record the chosen attributes. */
        $cand['qs'] = $qs;
        $cand['type'] = $type;
        $cand['charset'] = $charset;
        $cand['langs'] = '';
        foreach ($langs as $lang => $qs)
            $cand['langs'] .= ",$lang";
        $cand['langs'] = substr($cand['langs'], 1);
    }
    #unset($cand);
    #var_dump($cands);
    #printf("\nBest match: %d (%s)\n", $bestmatch, $cands[$bestmatch]['name']);

    if (count($cands) > 1) {
        $hdr = 'Vary: negotiate';
        if (count($avail_charsets) > 1)
            $hdr .= ',Accept-Charset';
        if (count($avail_langs) > 1)
            $hdr .= ',Accept-Language';
        if (count($avail_types) > 1)
            $hdr .= ',Accept';
        safe_header($hdr);
    }

    safe_header(strftime('Last-Modified: %a, %d %b %Y %H:%M:%S GMT', $ziptime));

    if (count($cands) > 1 && isset($negotiate['trans'])) {
        /* Provide a list of files. */
        http_response_code(300);
        safe_header('TCN: List');
        $hdr = 'Alternates:';
        $sep = ' ';
        foreach ($cands as $cand) {
            $hdr .= sprintf('%s{"%s" %0.5f}', $sep, basename($cand['name']),
                            $cand['oqs']);
            $hdr .= sprintf(' {type %s}', $cand['type']);
            $hdr .= sprintf(' {charset %s}', $cand['charset']);
            $hdr .= sprintf(' {language %s}', $cand['langs']);
        }
        safe_header($hdr);
    } else if (!isset($bestmatch)) {
        #error_log("HTTP_ACCEPT: ".$_SERVER['HTTP_ACCEPT']);
        #error_log("Accept-Language: ".print_r($accept_lang, TRUE));
        #error_log("Accept-Charset: ".print_r($accept_charset, TRUE));
        #error_log("Accept: ".print_r($accept_type, TRUE));
        #error_log("Candidates ".print_r($cands, TRUE));
        #error_log("Negotiate ".print_r($negotiate, TRUE));
        //error_log("Metadata ".print_r($meta, TRUE));
        // print "\nAccepted languages:\n";
        // var_dump($accept_lang);
        // print "\nAccepted charsets:\n";
        // var_dump($accept_charset);
        // print "\nAccepted types:\n";
        // var_dump($accept_type);

        /* Provide a list of files. */
        http_response_code(406);
        safe_header('TCN: List');
        $hdr = 'Alternates:';
        $sep = ' ';
        foreach ($cands as $cand) {
            $hdr .= sprintf('%s{"%s" %0.5f}', $sep, basename($cand['name']),
                            $cand['oqs']);
            $hdr .= sprintf(' {type %s}', $cand['type']);
            $hdr .= sprintf(' {charset %s}', $cand['charset']);
            $hdr .= sprintf(' {language %s}', $cand['langs']);
        }
        safe_header($hdr);
        header('Content-Type: text/plain; charset=UTF-8');
        print "There is nothing acceptable for your request.";
    } else {
        /* Select a file based on the user's preferences. */
        $fp = $zip->getStream($cands[$bestmatch]['name']);
        if ($fp === FALSE)
            die("No embedded file ".$cands[$bestmatch]['name']);
        http_response_code(200);
        if (count($cands) > 1)
            safe_header('Content-Location: '.basename($cands[$bestmatch]['name']));
        if (!empty($cands[$bestmatch]['langs']))
            safe_header('Content-Language: '.$cands[$bestmatch]['langs']);
        $hdr = 'Content-Type: '.$cands[$bestmatch]['type'];
        if (!empty($cands[$bestmatch]['charset']))
            $hdr .= '; charset='.$cands[$bestmatch]['charset'];
        safe_header($hdr);
        safe_header('Content-Length: '.$cands[$bestmatch]['size']);
        try {
            while (!feof($fp))
                print fread($fp, 1024);
        } finally {
            fclose($fp);
        }
    }

} finally {
    $zip->close();
}

?>
