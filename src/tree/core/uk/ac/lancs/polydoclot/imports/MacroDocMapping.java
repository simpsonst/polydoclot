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
 * Maps program elements to URIs based on a macro format.
 * 
 * @author simpsons
 */
public class MacroDocMapping implements DocMapping {
    private final URI docBase;
    private final MacroFormatter formatter;
    private final BiConsumer<? super Properties, ? super Element> propertySetter;

    /**
     * Create a mapping based on a macro format.
     * 
     * @param propertySetter a process to set the properties of its
     * first argument to the components describing the second
     * 
     * @param docBase the URI prefix of the external installation
     * 
     * @param formatter the macro format
     */
    public MacroDocMapping(BiConsumer<? super Properties, ? super Element> propertySetter,
                           URI docBase, MacroFormatter formatter) {
        this.propertySetter = propertySetter;
        this.docBase = docBase;
        this.formatter = formatter;
    }

    /**
     * {@inheritDoc}
     * 
     * @default Properties are generated from the element using the
     * converter provided to the constructor. They are then passed to
     * the formatter, and the result is resolved against the configured
     * base URI.
     */
    @Override
    public URI locate(Element elem) {
        Properties props = new Properties();
        propertySetter.accept(props, elem);
        return docBase.resolve(formatter.format(props));
    }
}
