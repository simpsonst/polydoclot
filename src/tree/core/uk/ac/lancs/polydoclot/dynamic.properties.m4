regexp(VERSION, `^\([0-9]+\)[^.]*\.\([0-9]+\)[^.]*\.\([0-9]+\)[^.]*\(\.\([0-9]+\)[^-]*-g\([0-9a-fA-F]+\)\)?$',
``revision='VERSION`
rcs=git
revision.version=\1
revision.minor=\2
revision.patchlevel=\3
'ifelse(`\5',,,``revision.build=\5
'')`'ifelse(`\6',,,``revision.commit=\6
'')`'')dnl
