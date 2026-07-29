facebook 1.2.1 Release POM

Published: 07 Apr 2026 | Maven Central

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.rudderstack.integration.kotlin</groupId>
  <artifactId>facebook</artifactId>
  <version>1.2.1</version>
  <packaging>aar</packaging>
  <name>Analytics Kotlin SDK</name>
  <description>RudderStack's SDK for android</description>
  <url>https://github.com/rudderlabs/rudder-sdk-kotlin</url>
  <licenses>
    <license>
      <name>MIT License</name>
      <url>https://github.com/rudderlabs/rudder-sdk-kotlin/blob/main/LICENSE.md</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <developers>
    <developer>
      <id>Rudderstack</id>
      <name>Rudderstack, Inc.</name>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:git://github.com/rudderlabs/rudder-sdk-kotlin.git</connection>
    <developerConnection>scm:git:git://github.com:rudderlabs/rudder-sdk-kotlin.git</developerConnection>
    <url>https://github.com/rudderlabs/rudder-sdk-kotlin/tree/main</url>
  </scm>
  <dependencies>
    <dependency>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-stdlib-jdk8</artifactId>
      <version>1.9.0</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>com.rudderstack.sdk.kotlin</groupId>
      <artifactId>android</artifactId>
      <version>[1.6.0, 2.0.0)</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>androidx.core</groupId>
      <artifactId>core-ktx</artifactId>
      <version>1.16.0</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.facebook.android</groupId>
      <artifactId>facebook-android-sdk</artifactId>
      <version>[18.0.1, 19.0.0)</version>
      <scope>runtime</scope>
    </dependency>
  </dependencies>
</project>
```
