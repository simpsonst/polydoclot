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

/**
 * Defines to what degree an element is deprecated.
 * 
 * @author simpsons
 */
enum Deprecation {
    /**
     * The element is not deprecated.
     */
    UNDEPRECATED {
        @Override
        public boolean isDeprecated() {
            return false;
        }

        @Override
        public boolean isExplicit() {
            return false;
        }
    },

    /**
     * The element is only deprecated because it is contained in a
     * deprecated element.
     */
    IMPLIED {
        @Override
        public boolean isDeprecated() {
            return true;
        }

        @Override
        public boolean isExplicit() {
            return false;
        }
    },

    /**
     * The element is marked deprecated (with {@link Deprecated
     * &#64;Deprecated} or <code>&#64;deprecated</code>, but does not
     * offer any alternative advice.
     */
    MARKED {
        @Override
        public boolean isDeprecated() {
            return true;
        }

        @Override
        public boolean isExplicit() {
            return true;
        }
    },

    /**
     * The element is tagged with alternative advice.
     */
    ADVISED {
        @Override
        public boolean isDeprecated() {
            return true;
        }

        @Override
        public boolean isExplicit() {
            return true;
        }
    };

    /**
     * Determine whether the element is deprecated at all.
     * 
     * @return {@code true} if the element is {@link #IMPLIED},
     * {@link #MARKED} or {@link #ADVISED}
     */
    public abstract boolean isDeprecated();

    /**
     * Determine whether the element is explicitly deprecated.
     * 
     * @return {@code true} if the element is {@link #MARKED} or
     * {@link #ADVISED}
     */
    public abstract boolean isExplicit();
}
