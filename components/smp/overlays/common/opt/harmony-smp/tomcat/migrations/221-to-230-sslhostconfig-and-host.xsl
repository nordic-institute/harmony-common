<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="infer-store-type">
        <xsl:param name="path"/>

        <xsl:variable name="lower" select="translate(normalize-space($path), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
        <xsl:variable name="len" select="string-length($lower)"/>

        <xsl:choose>
            <!-- PKCS12 -->
            <xsl:when test="$len &gt;= 4 and substring($lower, $len - 3) = '.p12'">
                <xsl:text>PKCS12</xsl:text>
            </xsl:when>
            <xsl:when test="$len &gt;= 4 and substring($lower, $len - 3) = '.pfx'">
                <xsl:text>PKCS12</xsl:text>
            </xsl:when>
            <xsl:when test="$len &gt;= 7 and substring($lower, $len - 6) = '.pkcs12'">
                <xsl:text>PKCS12</xsl:text>
            </xsl:when>

            <!-- JKS -->
            <xsl:when test="$len &gt;= 4 and substring($lower, $len - 3) = '.jks'">
                <xsl:text>JKS</xsl:text>
            </xsl:when>

            <xsl:otherwise/>
        </xsl:choose>
    </xsl:template>

    <!-- 1) TLS Legacy to SSLHostConfig -->
    <xsl:template match="Connector[@SSLEnabled='true'
                         and ( @keystoreFile or @keystorePass or @truststoreFile or @truststorePass )
                         and not(SSLHostConfig)]">

        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:if test="name() != 'keystoreFile'
                  and name() != 'keystorePass'
                  and name() != 'truststoreFile'
                  and name() != 'truststorePass'">
                    <xsl:copy/>
                </xsl:if>
            </xsl:for-each>

            <xsl:apply-templates select="node()"/>

            <SSLHostConfig hostName="_default_">
                <xsl:if test="@truststoreFile">
                    <xsl:attribute name="truststoreFile"><xsl:value-of select="@truststoreFile"/></xsl:attribute>
                </xsl:if>
                <xsl:if test="@truststorePass">
                    <xsl:attribute name="truststorePassword"><xsl:value-of select="@truststorePass"/></xsl:attribute>
                </xsl:if>
                <xsl:if test="@truststoreFile">
                    <xsl:variable name="tstype">
                        <xsl:call-template name="infer-store-type">
                            <xsl:with-param name="path" select="@truststoreFile"/>
                        </xsl:call-template>
                    </xsl:variable>

                    <xsl:if test="string-length(normalize-space($tstype)) &gt; 0">
                        <xsl:attribute name="truststoreType">
                            <xsl:value-of select="normalize-space($tstype)"/>
                        </xsl:attribute>
                    </xsl:if>
                </xsl:if>

                <Certificate>
                    <xsl:if test="@keystoreFile">
                        <xsl:attribute name="certificateKeystoreFile"><xsl:value-of select="@keystoreFile"/></xsl:attribute>
                    </xsl:if>
                    <xsl:if test="@keystorePass">
                        <xsl:attribute name="certificateKeystorePassword"><xsl:value-of select="@keystorePass"/></xsl:attribute>
                    </xsl:if>

                    <xsl:if test="@keystoreFile">
                        <xsl:variable name="kstype">
                            <xsl:call-template name="infer-store-type">
                                <xsl:with-param name="path" select="@keystoreFile"/>
                            </xsl:call-template>
                        </xsl:variable>

                        <xsl:if test="string-length(normalize-space($kstype)) &gt; 0">
                            <xsl:attribute name="certificateKeystoreType">
                                <xsl:value-of select="normalize-space($kstype)"/>
                            </xsl:attribute>
                        </xsl:if>
                    </xsl:if>
                </Certificate>
            </SSLHostConfig>
        </xsl:copy>
    </xsl:template>

    <!-- 2) Host migration -->
    <xsl:template match="Host[@name='localhost'
                         and @appBase='webapps'
                         and @unpackWARs='true'
                         and @autoDeploy='true'
                         and not(@workDir)
                         and not(@createDirs)]">

        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:choose>
                    <xsl:when test="name()='appBase'">
                        <xsl:attribute name="appBase">/opt/harmony-smp/webapps</xsl:attribute>
                    </xsl:when>
                    <xsl:when test="name()='unpackWARs'">
                        <xsl:attribute name="unpackWARs">false</xsl:attribute>
                    </xsl:when>
                    <xsl:when test="name()='autoDeploy'">
                        <xsl:attribute name="autoDeploy">false</xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>

            <xsl:attribute name="createDirs">false</xsl:attribute>
            <xsl:attribute name="workDir">/var/opt/harmony-smp/work</xsl:attribute>

            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
