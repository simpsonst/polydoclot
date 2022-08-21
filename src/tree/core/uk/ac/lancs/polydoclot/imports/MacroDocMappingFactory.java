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
import java.util.function.BiConsumer;

import javax.lang.model.element.Element;

import uk.ac.lancs.polydoclot.util.MacroFormatter;

/**
 * Crayes a macro-based document mapping that uses a property from the
 * configuration. If the property is not present, it can use a default.
 * 
 * @author simpsons
 */
public final class MacroDocMappingFactory implements DocMappingFactory {
    private final BiConsumer<? super Properties, ? super Element> propertySetter;
    private final String propertyName;
    private final String defaultValue;

    /**
     * Create a factory to extract the mapping from a given property,
     * with no default scheme.
     * 
     * @param propertySetter a process to set the properties of its
     * first argument to the components describing the second
     * 
     * @param types utilities for manipulating types
     *
     * @param propertyName the name of the property to use
     */
    public MacroDocMappingFactory(BiConsumer<? super Properties, ? super Element> propertySetter,
                                  String propertyName) {
        this(propertySetter, propertyName, null);
    }

    /**
     * Create a factory to extract the mapping from a given property,
     * using a default if not specified.
     * 
     * @param propertySetter a process to set the properties of its
     * first argument to the components describing the second
     *
     * @param propertyName the name of the property to use
     *
     * @param other the back-up factory if the property is not set
     */
    public MacroDocMappingFactory(BiConsumer<? super Properties, ? super Element> propertySetter,
                                  String propertyName, String defaultValue) {
        this.propertySetter = propertySetter;
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    @Override
    public DocMapping createMapping(URI docBase, URI cache,
                                    Properties config) {
        String scheme = config.getProperty(propertyName, defaultValue);
        if (scheme == null) return null;
        MacroFormatter format = new MacroFormatter(scheme);
        return new MacroDocMapping(propertySetter, docBase, format);
    }
}
