regexp(VERSION, `^\([0-9]+\)\([^.]\)?\.\([0-9]+\)\([^.]\)?\.\([0-9]+\)[^.-]*\(-\([0-9]+[^-]*\)-g\([0-9a-fA-F]+\)\)?',
``revision.version=\1
revision.minor=\3
revision.patchlevel=\5
'ifelse(`\7',,,``revision.ahead=\7
'')`'ifelse(`\8',,,``revision.commit=\8
'')`'')dnl
