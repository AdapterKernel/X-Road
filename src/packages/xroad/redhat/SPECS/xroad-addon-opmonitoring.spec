# do not repack jars
%define __jar_repack %{nil}
# produce .elX dist tag on both centos and redhat
%define dist %(/usr/lib/rpm/redhat/dist.sh)

Name:       xroad-addon-opmonitoring
Version:    %{xroad_version}
Release:    %{rel}%{?snapshot}%{?dist}
Summary:    X-Road AddOn: opmonitoring
Group:      Applications/Internet
License:    MIT
Requires:   xroad-proxy >= %version, xroad-opmonitor >= %version

%define src %{_topdir}/..

%description
AddOn for operations monitoring.

%prep

%build

%install
mkdir -p %{buildroot}/usr/share/xroad/jlib/addon/proxy/
mkdir -p %{buildroot}/usr/share/doc/%{name}

cp -p %{src}/addon/proxy/opmonitoring.conf %{buildroot}/usr/share/xroad/jlib/addon/proxy/
cp -p %{src}/../../addons/op-monitoring/build/libs/op-monitoring-1.0.jar %{buildroot}/usr/share/xroad/jlib/addon/proxy/
cp -p %{src}/../../LICENSE.txt %{buildroot}/usr/share/doc/xroad-addon-opmonitoring/
cp -p %{src}/../../securityserver-LICENSE.info %{buildroot}/usr/share/doc/xroad-addon-opmonitoring/
cp -p %{src}/../../../CHANGELOG.md %{buildroot}/usr/share/doc/xroad-addon-opmonitoring/

%clean
rm -rf %{buildroot}

%files
%defattr(-,xroad,xroad,-)
/usr/share/xroad/jlib/addon/proxy/op-monitoring-1.0.jar
/usr/share/xroad/jlib/addon/proxy/opmonitoring.conf
%doc /usr/share/doc/%{name}/LICENSE.txt
%doc /usr/share/doc/%{name}/securityserver-LICENSE.info
%doc /usr/share/doc/%{name}/CHANGELOG.md

%postun
%systemd_postun_with_restart xroad-proxy.service

%changelog

