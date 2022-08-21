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

package uk.ac.lancs.polydoclot.html;

/**
 * Retains a changing upmarker. This class is intended for methods that
 * require an upmarker context, and terminate with a different one,
 * which the caller must have access to, yet the method already returns
 * some other (unrelated) kind of information.
 * 
 * @author simpsons
 */
public final class Cursor {
    private Upmarker value;

    Cursor(Upmarker value) {
        this.value = value;
    }

    /**
     * Create a cursor for an upmarker.
     * 
     * @param value the initial value
     * 
     * @return the new cursor
     * 
     * @throws NullPointerException if the initial value is {@code null}
     * 
     * @constructor
     */
    public static Cursor of(Upmarker value) {
        if (value == null) throw new NullPointerException();
        return new Cursor(value);
    }

    /**
     * Get the current value of the cursor.
     * 
     * @return the current value
     */
    public Upmarker get() {
        return value;
    }

    /**
     * Set the current value of the cursor.
     * 
     * @param value the new value
     */
    public void set(Upmarker value) {
        this.value = value;
    }
}
