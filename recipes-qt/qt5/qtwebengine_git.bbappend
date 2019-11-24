inherit ccache


PACKAGECONFIG_append += "ffmpeg libwebp opus libvpx proprietary-codecs pepper-plugins"

pkg_postinst_ontarget_${PN}() {
		patchelf --replace-needed ${STAGING_LIBDIR}/libGLESv2.so libGLESv2.so /usr/lib/libQt*
		patchelf --replace-needed ${STAGING_LIBDIR}/libEGL.so libEGL.so /usr/lib/libQt*
}
