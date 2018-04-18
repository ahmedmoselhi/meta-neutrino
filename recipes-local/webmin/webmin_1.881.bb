SUMMARY = "Web-based administration interface"
HOMEPAGE = "http://www.webmin.com"
LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://LICENCE;md5=0373ac9f611e542ddebe1ec6394afc3c"

SRC_URI = "${SOURCEFORGE_MIRROR}/webadmin/webmin-${PV}.tar.gz \
           file://setup.sh \
           file://init-exclude.patch \
           file://net-generic.patch \
           file://remove-startup-option.patch \
           file://disable-version-check.patch \
           file://nfs-export.patch \
           file://exports-lib.pl.patch \
           file://mount-excludefs.patch \
           file://samba-config-fix.patch \
           file://proftpd-config-fix.patch \
           file://net-lib.pl.patch \
           file://media-tomb.patch \
           file://remove-python2.3.patch \
           file://mysql-config-fix.patch \
           file://webmin.service \
            "

SRC_URI[md5sum] = "f37b564c76c1c6b0241fccb1f844f2f0"
SRC_URI[sha256sum] = "6a93a74ff9adb0ca48cb8e03d74faf77731008eaca2613db225e1d59e07d5190"

UPSTREAM_CHECK_URI = "http://www.webmin.com/download.html"
UPSTREAM_CHECK_REGEX = "webmin-(?P<pver>\d+(\.\d+)+).tar.gz"

inherit perlnative systemd

do_configure() {
    # Remove binaries and plugins for other platforms
    rm -rf acl/Authen-SolarisRBAC-0.1*
    rm -rf format bsdexports hpuxexports sgiexports
    rm -rf zones rbac smf ipfw ipfilter dfsadmin
    rm -f mount/freebsd-mounts* mount/netbsd-mounts*
    rm -f mount/openbsd-mounts* mount/macos-mounts*

    # Remove some plugins for the moment
    rm -rf lilo frox wuftpd telnet pserver cpan shorewall webalizer cfengine fsdump pap
    rm -rf majordomo fetchmail sendmail mailboxes procmail filter mailcap dovecot exim spam qmailadmin postfix
    rm -rf stunnel squid sarg pptp-client pptp-server jabber openslp sentry cluster-* vgetty burner heartbeat

    # Adjust configs
    [ -f init/config-debian-linux ] && mv init/config-debian-linux init/config-generic-linux
    sed -i "s/shutdown_command=.*/shutdown_command=poweroff/" init/config-generic-linux
    echo "exclude=bootmisc.sh,single,halt,reboot,hostname.sh,modutils.sh,mountall.sh,mountnfs.sh,networking,populate-volatile.sh,rmnologin.sh,save-rtc.sh,umountfs,umountnfs.sh,hwclock.sh,checkroot.sh,banner.sh,udev,udev-cache,devpts.sh,psplash.sh,sendsigs,fbsetup,bootlogd,stop-bootlogd,sysfs.sh,syslog,syslog.busybox,urandom,webmin,functions.initscripts,read-only-rootfs-hook.sh" >> init/config-generic-linux
    echo "excludefs=devpts,devtmpfs,usbdevfs,proc,tmpfs,sysfs,debugfs" >> mount/config-generic-linux

    [ -f exports/config-debian-linux ] && mv exports/config-debian-linux exports/config-generic-linux
    sed -i "s/killall -HUP rpc.nfsd && //" exports/config-generic-linux
    sed -i "s/netstd_nfs/nfsserver/g" exports/config-generic-linux

    # Fix insane naming that causes problems at packaging time (must be done before deleting below)
    find . -name "*\**" | while read from
    do
        to=`echo "$from" | sed "s/*/ALL/"`
        mv "$from" "$to"
    done

    # Remove some other files we don't need
    find . -name "config-*" -a \! -name "config-generic-linux" -a \! -name "config-ALL-linux" -a \! -name "*.pl" -delete
    find . -regextype posix-extended -regex ".*/(openserver|aix|osf1|osf|openbsd|netbsd|freebsd|unixware|solaris|macos|irix|hpux|cygwin|windows)-lib\.pl" -delete
    rm -f webmin-gentoo-init webmin-caldera-init webmin-debian-pam webmin-pam

    # Don't need these at runtime (and we have our own setup script)
    rm -f setup.sh
    rm -f setup.pl

    # Use pidof for finding PIDs
    sed -i "s/find_pid_command=.*/find_pid_command=pidof NAME/" config-generic-linux
}

WEBMIN_LOGIN ?= "root"
WEBMIN_PASSWORD ?= "root"

do_install() {
    install -d ${D}${sysconfdir}
    install -d ${D}${sysconfdir}/webmin

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${WORKDIR}/webmin.service ${D}${systemd_unitdir}/system
    sed -i -e 's,@SYSCONFDIR@,${sysconfdir},g' \
           ${D}${systemd_unitdir}/system/webmin.service

    install -d ${D}${localstatedir}
    install -d ${D}${localstatedir}/webmin

    install -d ${D}${libexecdir}/webmin
    cd ${S} || exit 1
    tar --no-same-owner --exclude='./patches' --exclude='./.pc' -cpf - . \
        | tar --no-same-owner -xpf - -C ${D}${libexecdir}/webmin

    rm -f ${D}${libexecdir}/webmin/webmin-init
    rm -f ${D}${libexecdir}/webmin/ajaxterm/ajaxterm/configure.initd.gentoo
    rm -rf ${D}${libexecdir}/webmin/patches

    # Run setup script
    export perl=perl
    export perl_runtime=${bindir}/perl
    export prefix=${D}
    export tempdir=${S}/install_tmp
    export wadir=${libexecdir}/webmin
    export config_dir=${sysconfdir}/webmin
    export var_dir=${localstatedir}/webmin
    export os_type=generic-linux
    export os_version=0
    export real_os_type="${DISTRO_NAME}"
    export real_os_version="${DISTRO_VERSION}"
    export port=10000
    export login=${WEBMIN_LOGIN}
    export password=${WEBMIN_PASSWORD}
    export ssl=0
    export atboot=1
    export no_pam=1
    mkdir -p $tempdir
    ${S}/../setup.sh

    # Ensure correct PERLLIB path
    sed -i -e 's#${D}##g' ${D}${sysconfdir}/webmin/start
}

SYSTEMD_SERVICE_${PN} = "webmin.service"
SYSTEMD_AUTO_ENABLE_${PN} = "enable"

DEPENDS += "perl smartmontools procps mdadm"

# FIXME: some of this should be figured out automatically
RDEPENDS_${PN} += "perl perl-module-socket perl-module-exporter perl-module-exporter-heavy perl-module-carp perl-module-strict"
RDEPENDS_${PN} += "perl-module-warnings perl-module-xsloader perl-module-posix perl-module-autoloader"
RDEPENDS_${PN} += "perl-module-fcntl perl-module-tie-hash perl-module-vars perl-module-time-local perl-module-config perl-module-constant"
RDEPENDS_${PN} += "perl-module-file-glob perl-module-file-copy perl-module-sdbm-file perl-module-feature smartmontools"

PACKAGES_DYNAMIC += "webmin-module-* webmin-theme-*"
RRECOMMENDS_${PN} += "webmin-module-system-status net-ssleay-perl perl-module-file-path webmin-module-mount gnupg webmin-module-samba webmin-theme-authentic-theme \
webmin-module-change-user webmin-module-file webmin-module-net webmin-module-pam webmin-module-shell webmin-module-smart-status webmin-module-sshd webmin-module-status \
webmin-module-system-status webmin-module-webmin webmin-module-webminlog webmin-module-updown webmin-module-acl webmin-module-servers webmin-module-filemin \
webmin-module-fdisk webmin-module-exports webmin-module-useradmin webmin-module-passwd \
"

PACKAGES += "${PN}-module-proc"
RDEPENDS_${PN}-module-proc = "procps"
RDEPENDS_${PN}-module-raid = "mdadm"
RDEPENDS_${PN}-module-filemin = "perl-module-perlio"
RDEPENDS_${PN}-module-exports = "perl-module-file-basename perl-module-file-path perl-module-cwd perl-module-file-spec perl-module-file-spec-unix"
RDEPENDS_${PN}-theme-authentic-theme = "perl-module-lib perl-module-overload perl-module-bytes perl-module-encode perl-module-encode-unicode perl-module-utf8 perl-module-unicore"
RRECOMMENDS_${PN}-module-fdisk = "parted"
RRECOMMENDS_${PN}-module-lvm = "lvm2"

python populate_packages_prepend() {
    import os, os.path

    wadir = bb.data.expand('${libexecdir}/webmin', d)
    wadir_image = bb.data.expand('${D}', d) + wadir
    modules = []
    themes = []
    for mod in os.listdir(wadir_image):
        modinfo = os.path.join(wadir_image, mod, "module.info")
        themeinfo = os.path.join(wadir_image, mod, "theme.info")
        if os.path.exists(modinfo):
            modules.append(mod)
        elif os.path.exists(themeinfo):
            themes.append(mod)

    do_split_packages(d, wadir, '^(%s)$' % "|".join(modules), 'webmin-module-%s', 'Webmin module for %s', allow_dirs=True, prepend=True)
    do_split_packages(d, wadir, '^(%s)$' % "|".join(themes), 'webmin-theme-%s', 'Webmin theme for %s', allow_dirs=True, prepend=True)
}

# Time-savers
package_do_pkgconfig() {
    :
}