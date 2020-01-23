# Copyright (c) 2019-2020 DINSIC, Bastien Guerry <bastien.guerry@data.gouv.fr>
# SPDX-License-Identifier: EPL-2.0
# License-Filename: LICENSES/EPL-2.0.txt

FROM java:8-alpine
ENV SMTP_HOST ${SMTP_HOST}
ENV SMTP_LOGIN ${SMTP_LOGIN}
ENV SMTP_PASSWORD ${SMTP_PASSWORD}
ENV SILLWEB_ADMIN_EMAIL ${SILLWEB_ADMIN_EMAIL}
ENV SILLWEB_FROM ${SILLWEB_FROM}
ENV SILLWEB_PORT ${SILLWEB_PORT}
ENV SILLWEB_MSGID_DOMAIN ${SILLWEB_MSGID_DOMAIN}
ADD target/sillweb-standalone.jar /sillweb/sillweb-standalone.jar
CMD ["java", "-jar", "/sillweb/sillweb-standalone.jar"]
