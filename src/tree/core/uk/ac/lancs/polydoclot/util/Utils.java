/*
 * Copyright 2018,2019, Lancaster University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 * 
 *  * Neither the name of the copyright holder nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.polydoclot.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Holds miscellaneous utilities that are not doclet-specific.
 * 
 * @author simpsons
 */
public final class Utils {
    private Utils() {}

    /**
     * Check whether a given locale is suitable for another.
     * 
     * @param required the context locale
     * 
     * @param offered the offered locale
     * 
     * @return {@code true} iff they are compatible
     */
    public static boolean isCompatible(Locale required, Locale offered) {
        if (!required.getLanguage().equalsIgnoreCase(offered.getLanguage()))
            return false;
        String offeredCountry = offered.getCountry();
        if (offeredCountry == null || offeredCountry.equals("")) return true;
        if (!required.getCountry().equalsIgnoreCase(offeredCountry))
            return false;
        String offeredVariant = offered.getVariant();
        if (offeredVariant == null || offeredVariant.equals("")) return true;
        return !required.getVariant().equalsIgnoreCase(offeredVariant);
    }

    /**
     * Create a URI which, when resolved against one URI, yields
     * another. In other words, {@code r} is returned such that
     * {@code base.resolve(r).equals(target)}. A relative URI is
     * generated if possible, with the fewest path elements.
     * 
     * <p>
     * This should be used instead of base.relativize(target), which is
     * buggy.
     * 
     * @param base the base URI to relative against
     * 
     * @param target the target URI to relative
     * 
     * @return the shortest URI that identifies the target relative to
     * the base
     */
    public static URI relativize(URI base, URI target) {
        base = base.normalize();
        target = target.normalize();

        if (target.isOpaque() || base.isOpaque()) return target;

        if (target.getScheme() == null ^ base.getScheme() == null)
            return target;

        if (target.getAuthority() == null ^ base.getAuthority() == null)
            return target;

        /* TODO: This should really use the private
         * URI.equalIgnoreCase(String,String) method. */
        if (target.getScheme() != null
            && !target.getScheme().equalsIgnoreCase(base.getScheme()))
            return target;

        /* TODO: This should really use the private
         * URI.equal(String,String) method. */
        if (target.getAuthority() != null
            && !target.getAuthority().equals(base.getAuthority()))
            return target;

        /* TODO: These paths should be first normalized. */
        String basePath = base.getPath();
        StringBuilder targetPath = new StringBuilder(target.getPath());

        /* Find the longest common initial sequence of path elements. */
        int length = Math.min(basePath.length(), targetPath.length());
        int diff = 0;
        for (int i = 0; i < length; i++) {
            char c = basePath.charAt(i);
            if (c != targetPath.charAt(i)) break;
            if (c == '/') diff = i + 1;
        }

        /* Remove the common elements from the target, including their
         * trailing slashes. */
        targetPath.delete(0, diff);

        /* Count remaining complete path elements in the base, prefixing
         * the target with "../" for each one. */
        for (int slash = basePath.indexOf('/', diff); slash > -1; slash =
            basePath.indexOf('/', slash + 1))
            targetPath.insert(0, "../");

        /* Make sure the resultant path is not empty. */
        if (targetPath.length() == 0) targetPath.append("./");

        /* Build the new URI from the computed path. */
        try {
            return new URI(null, null, targetPath.toString(),
                           target.getQuery(), target.getFragment());
        } catch (URISyntaxException ex) {
            /* Shouldn't happen. */
            throw new AssertionError("unreachable");
        }
    }

    /**
     * Assume that a user-provided URI is meant to represent a
     * 'directory', i.e. that it should end in a forward slash. If it
     * doesn't, one is appended.
     * 
     * @param in the user-supplied URI
     * 
     * @return the same URI if it already ends with a slash; or else a
     * new one with a slash appended
     */
    public static URI assumeDirectory(URI in) {
        if (in.toString().endsWith("/")) return in;
        return URI.create(in.toString() + "/");
    }

    /**
     * Determine whether a user-provided string is meant to be
     * interpreted as a local file name or a URI. If the first character
     * is a forward slash, it is treated as a filename, and converted to
     * a 'file:' URL; otherwise it is parsed as a URI.
     * 
     * @param str the user-provided string
     * 
     * @return a {@link Path} of the string, then converted to a URI, if
     * the string starts with a slash; otherwise, a URI formed directly
     * from the string
     */
    public static URI fileOrURI(String str) {
        return str.startsWith("/") ? Paths.get(str).toUri() : URI.create(str);
    }

    /**
     * Determine whether a user-provided string is meant to be
     * interpreted as a local file name or a URI. If the first character
     * is a forward slash, it is treated as a filename, and converted to
     * a 'file:' URL; otherwise it is parsed as a URI.
     * 
     * <p>
     * This method is very UNIX-oriented, and should be made more
     * portable.
     * 
     * @param str the user-provided string
     * 
     * @return a {@link Path} of the string, then converted to a URI, if
     * the string starts with a slash; a {@link Path} of the string,
     * made absolute and then converted to a URI if the string starts
     * with a dot; otherwise, a URI formed directly from the string
     */
    public static URI absoluteFileOrURI(String str) {
        if (str.startsWith("/")) return Paths.get(str).toUri();
        if (str.startsWith("."))
            return Paths.get(str).toAbsolutePath().toUri();
        return URI.create(str);
    }

}
