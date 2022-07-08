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
 *  * Neither the name of Lancaster University nor the names of
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

package uk.ac.lancs.polydoclot;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Specifies a content-negotiated slice of the documentation to
 * generate.
 * 
 * @author simpsons
 */
public final class SliceSpecification {
    /**
     * The locale used to select language-dependent text
     */
    public final Locale locale;

    /**
     * The character encoding for files generated in this slice
     */
    public final Charset charset;

    /**
     * The suffix used to distinguish this slice from other slices
     */
    public final String suffix;

    /**
     * Create a slice specification.
     * 
     * @param locale the locale used to select language-dependent text,
     * or {@code null} to use the environmental default
     * 
     * @param suffix the suffix used to distinguish this slice from
     * others, or {@code null} to use no distinguishing suffix
     * 
     * @param charset the character encoding for files generated in this
     * slice, or {@code null} to use UTF-8 as the default
     */
    public SliceSpecification(Locale locale, Charset charset, String suffix) {
        this.locale = locale != null ? locale : Locale.getDefault();
        this.suffix = suffix != null ? suffix : "";
        this.charset = charset != null ? charset : StandardCharsets.UTF_8;
    }
}
