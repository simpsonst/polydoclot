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

package uk.ac.lancs.polydoclot.imports;

import java.net.URI;
import java.util.Properties;

/**
 * Creates and configures documentation mappings.
 * 
 * @author simpsons
 */
public interface DocMappingFactory {
    /**
     * Create a new mapping.
     *
     * @param docBase the base URI of documentation, i.e. the URI of the
     * <samp>{@value DocImport#PACKAGE_LIST_NAME}</samp> file, or any
     * sibling thereof. For example: for JSE6,
     * <samp>http://java.sun.com/javase/6/docs/api/</samp> &mdash; the
     * trailing slash is important for resolution of relative URIs.
     *
     * @param cache Relative URIs extracted from the configuration
     * should be resolved against this URI.
     *
     * @param config configuration parameters for the mapping. These are
     * taken from the <samp>{@value DocImport#PROPERTIES_NAME}</samp>
     * file in the documentation.
     * 
     * @return a new documentation mapping for the given resources
     */
    DocMapping createMapping(URI docBase, URI cache, Properties config);
}
