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

import java.util.List;

import com.sun.source.doctree.DocTree;

/**
 * Determines how much detail of an element link or type link should be
 * shown.
 * 
 * @author simpsons
 */
public abstract class LinkContent {
    /**
     * Determines how much auto-generated context for a class or member
     * should be shown.
     * 
     * @author simpsons
     */
    public static enum Context {
    /**
     * No context should be shown.
     */
    NONE {
        @Override
        public boolean isShowingPackage() {
            return false;
        }

        @Override
        public boolean isShowingContainers() {
            return false;
        }

        @Override
        public boolean isShowingNecessaryContainers() {
            return false;
        }
    },

    /**
     * Only containing classes necessary to reach the output context
     * class should be shown.
     */
    NECESSARY_CONTAINERS {
        @Override
        public boolean isShowingPackage() {
            return false;
        }

        @Override
        public boolean isShowingNecessaryContainers() {
            return true;
        }

        @Override
        public boolean isShowingContainers() {
            return false;
        }
    },

    /**
     * All containing classes should be shown, but no package.
     */
    ALL_CONTAINERS {
        @Override
        public boolean isShowingPackage() {
            return false;
        }

        @Override
        public boolean isShowingContainers() {
            return true;
        }

        @Override
        public boolean isShowingNecessaryContainers() {
            return true;
        }
    },

    /**
     * All containing classes and the package should be shown.
     */
    PACKAGE {
        @Override
        public boolean isShowingPackage() {
            return true;
        }

        @Override
        public boolean isShowingContainers() {
            return true;
        }

        @Override
        public boolean isShowingNecessaryContainers() {
            return true;
        }
    };

        /**
         * Determine whether the containing package should be shown.
         * 
         * @return {@code true} if the containing package should be
         * shown
         */
        public abstract boolean isShowingPackage();

        /**
         * Determine whether containing classes that are needed for
         * context should be shown.
         * 
         * @return {@code true} if the containing classes should be
         * shown for necessary context
         */
        public abstract boolean isShowingNecessaryContainers();

        /**
         * Determine whether containing classes should be shown whether
         * necessary or not.
         * 
         * @return {@code true} if the containing classes should be
         * shown regardless
         */
        public abstract boolean isShowingContainers();
    }

    /**
     * Determine how much context for a class or member should be shown
     * before it.
     * 
     * @return the amount of context to show
     */
    public abstract Context getContainerContext();

    /**
     * Determine whether containers outside of the output context
     * container should be shown.
     * 
     * @return {@code true} iff containers outside of the output context
     * container should be shown
     */
    public final boolean isShowingNecessaryContainers() {
        return getContainerContext().isShowingNecessaryContainers();
    }

    /**
     * Determine whether the containing classes should be shown.
     * 
     * @return {@code true} iff the containing classes should be shown
     */
    public final boolean isShowingContainers() {
        return getContainerContext().isShowingContainers();
    }

    /**
     * Determine whether the containing package should be shown.
     * 
     * @return {@code true} iff the containing package should be shown
     */
    public final boolean isShowingPackage() {
        return getContainerContext().isShowingPackage();
    }

    /**
     * Determine whether method/constructor parameters should be shown.
     * 
     * @return {@code true} iff parameters should be shown
     */
    public abstract boolean isShowingParameters();

    /**
     * Determine whether a Javadoc label should be shown.
     * 
     * @return {@code true} iff a label should be shown
     */
    public abstract boolean hasLabel();

    /**
     * Get the label.
     * 
     * @return the label as parsed Javadoc source, or {@code null} if
     * not present
     */
    public abstract List<? extends DocTree> label();

    /**
     * Get the source context for the label.
     * 
     * @return the label's source context, or {@code null} if no label
     * is present
     */
    public abstract SourceContext sourceContext();

    private static class FilterLinkContent extends LinkContent {
        protected final LinkContent base;

        public FilterLinkContent(LinkContent base) {
            this.base = base;
        }

        @Override
        public boolean isShowingParameters() {
            return base.isShowingParameters();
        }

        @Override
        public boolean hasLabel() {
            return base.hasLabel();
        }

        @Override
        public List<? extends DocTree> label() {
            return base.label();
        }

        @Override
        public Context getContainerContext() {
            return base.getContainerContext();
        }

        @Override
        public SourceContext sourceContext() {
            return base.sourceContext();
        }
    }

    /**
     * Disable display of method/constructor parameters.
     * 
     * @return the new detail specification
     */
    public final LinkContent withoutParameters() {
        if (!isShowingParameters()) return this;
        return new FilterLinkContent(this) {
            @Override
            public boolean isShowingParameters() {
                return false;
            }
        };
    }

    /**
     * Enable display of method/constructor parameters and disable the
     * label.
     * 
     * @return the new detail specification
     */
    public final LinkContent withParameters() {
        if (isShowingParameters()) return this;
        return new FilterLinkContent(this) {
            @Override
            public boolean isShowingParameters() {
                return true;
            }

            @Override
            public boolean hasLabel() {
                return false;
            }

            @Override
            public List<? extends DocTree> label() {
                throw new UnsupportedOperationException("should not be invoked");
            }

            @Override
            public SourceContext sourceContext() {
                throw new UnsupportedOperationException("should not be invoked");
            }
        };
    }

    /**
     * Disable display of the package.
     * 
     * @return the new detail specification
     */
    public final LinkContent withoutPackage() {
        if (!isShowingPackage()) return this;
        if (isShowingContainers()) return new FilterLinkContent(this) {
            @Override
            public Context getContainerContext() {
                return Context.ALL_CONTAINERS;
            }
        };
        return new FilterLinkContent(this) {
            @Override
            public Context getContainerContext() {
                return Context.NONE;
            }
        };
    }

    /**
     * Enable the disable of the package, if no label has been provided.
     * 
     * @return the new content with packages enabled, or this object if
     * it already has a label
     */
    public final LinkContent withPackageIfNoLabel() {
        if (hasLabel()) return this;
        return withPackage();
    }

    /**
     * Enable display of the package and disable the label.
     * 
     * @return the new detail specification
     */
    public final LinkContent withPackage() {
        if (isShowingPackage()) return this;
        return new FilterLinkContent(this) {
            @Override
            public Context getContainerContext() {
                return Context.PACKAGE;
            }

            @Override
            public boolean hasLabel() {
                return false;
            }

            @Override
            public List<? extends DocTree> label() {
                throw new UnsupportedOperationException("should not be invoked");
            }

            @Override
            public SourceContext sourceContext() {
                throw new UnsupportedOperationException("should not be invoked");
            }
        };
    }

    /**
     * Disable display of the containers.
     * 
     * @return the new detail specification
     */
    public final LinkContent withoutContainers() {
        if (!isShowingContainers()) return this;
        return new FilterLinkContent(this) {
            @Override
            public Context getContainerContext() {
                return Context.NONE;
            }
        };
    }

    /**
     * Disable display of non-essential containers.
     * 
     * @return the new detail specification
     */
    public final LinkContent withoutNonessentialContainers() {
        if (!isShowingNecessaryContainers()) return this;
        return new FilterLinkContent(this) {
            @Override
            public Context getContainerContext() {
                return Context.NECESSARY_CONTAINERS;
            }
        };
    }

    /**
     * Enable display of the containers and disable the label.
     * 
     * @return the new detail specification
     */
    public final LinkContent withContainers() {
        if (isShowingContainers()) return this;
        return new FilterLinkContent(this) {
            @Override
            public Context getContainerContext() {
                return Context.ALL_CONTAINERS;
            }

            @Override
            public boolean hasLabel() {
                return false;
            }

            @Override
            public List<? extends DocTree> label() {
                throw new UnsupportedOperationException("should not be invoked");
            }

            @Override
            public SourceContext sourceContext() {
                throw new UnsupportedOperationException("should not be invoked");
            }
        };
    }

    /**
     * The normal level of detail, in which parameters and containing
     * classes are shown.
     */
    public static final LinkContent NORMAL = new LinkContent() {
        @Override
        public List<? extends DocTree> label() {
            throw new UnsupportedOperationException("should not be invoked");
        }

        @Override
        public boolean isShowingParameters() {
            return true;
        }

        @Override
        public boolean hasLabel() {
            return false;
        }

        @Override
        public Context getContainerContext() {
            return Context.ALL_CONTAINERS;
        }

        @Override
        public SourceContext sourceContext() {
            throw new UnsupportedOperationException("should not be invoked");
        }
    };

    /**
     * Specify the content of a link to be user-defined Javadoc content.
     * 
     * @param sourceContext the source context for context-sensitive
     * information provided in the Javadoc content
     * 
     * @param label the Javadoc content
     * 
     * @return detail that specifies the use of the supplied Javadoc
     * content as link content
     */
    public static LinkContent forLabel(SourceContext sourceContext,
                                       List<? extends DocTree> label) {
        if (label == null || label.isEmpty()) return NORMAL;
        return new LinkContent() {
            @Override
            public List<? extends DocTree> label() {
                return label;
            }

            @Override
            public boolean isShowingParameters() {
                return false;
            }

            @Override
            public boolean hasLabel() {
                return true;
            }

            @Override
            public Context getContainerContext() {
                return Context.NONE;
            }

            @Override
            public SourceContext sourceContext() {
                return sourceContext;
            }
        };
    }
}
