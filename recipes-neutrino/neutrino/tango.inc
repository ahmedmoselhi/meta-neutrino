
SRC_URI_append += "git://github.com/TangoCash/neutrino-mp-tangos.git;protocol=https \
"


EXTRA_OECONF_append += "--enable-reschange \
                        --with-stb-hal-includes=${STAGING_INCDIR}/libstb-hal \
			--enable-lcd4linux \
"
