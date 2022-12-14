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

package uk.ac.lancs.polydoclot;

import java.net.URI;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Identifies a referenced package, class or member.
 * 
 * @author simpsons
 */
public final class ElementReference {
    /**
     * The referenced package, or the package of the referenced element,
     * or {@code null} if the element is imported
     */
    public final PackageElement packageElement;

    /**
     * The referenced class, or the containing class of the referenced
     * element, or {@code null} if the element is imported
     */
    public final TypeElement typeElement;

    /**
     * The referenced field, or {@code null} if the element is imported
     * or is not a field
     */
    public final VariableElement variableElement;

    /**
     * The referenced constructor or method, or {@code null} if the
     * element is imported or is not an executable member
     */
    public final ExecutableElement executableElement;

    /**
     * The location of the page documenting the element
     */
    public final URI location;

    ElementReference(PackageElement packageElement, TypeElement typeElement,
                     VariableElement variableElement,
                     ExecutableElement executableElement, URI location) {
        this.packageElement = packageElement;
        this.typeElement = typeElement;
        this.variableElement = variableElement;
        this.executableElement = executableElement;
        this.location = location;
    }
}
