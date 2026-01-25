# CI/CD & Release Pipeline PRD: akka-openapi-maven-plugin

## Executive Summary

This document defines the complete Continuous Integration/Continuous Deployment (CI/CD) and release pipeline for **akka-openapi-maven-plugin** using GitHub Actions and Maven.

**Key Goals**:
- ✅ Automated testing on every pull request
- ✅ Automated releases to Maven Central via semantic versioning
- ✅ GPG code signing for security
- ✅ One-command release process (git tag → published on Maven Central)
- ✅ Professional release notes and GitHub releases
- ✅ Zero manual steps once configured

---

## 1. CI/CD Architecture Overview

### 1.1 Pipeline Stages

```
┌──────────────────────────────────────────────────────────────────┐
│                     Developer Workflow                             │
└──────────────────────────────────────────────────────────────────┘
                              ↓
                    Create Feature Branch
                              ↓
        ┌─────────────────────────────────┐
        │  Push to GitHub (PR triggers CI)  │
        └─────────────────────────────────┘
                              ↓
    ┌─────────────────────────────────────────────────────┐
    │  GitHub Actions: CI Workflow (PR Validation)         │
    │  - Build & Compile                                   │
    │  - Unit Tests (90%+ coverage)                         │
    │  - Integration Tests                                 │
    │  - SonarQube Code Quality Analysis                   │
    │  - CheckStyle & FindBugs                             │
    │  - Dependency Check for vulnerabilities              │
    └─────────────────────────────────────────────────────┘
                              ↓
                    All Checks Pass? ✅
                              ↓
                    Merge to Main (via PR)
                              ↓
        ┌──────────────────────────────────────────┐
        │  Create GitHub Release Tag (vX.Y.Z)       │
        │  (e.g., git tag v1.2.0 && git push)      │
        └──────────────────────────────────────────┘
                              ↓
    ┌─────────────────────────────────────────────────────┐
    │  GitHub Actions: Release Workflow (Tag triggers)     │
    │  - Build Release Artifacts                           │
    │  - GPG Sign JAR/POM/Source                           │
    │  - Deploy to Maven Central Staging                   │
    │  - Deploy to Maven Central Release (auto-promote)    │
    │  - Create GitHub Release Notes                       │
    └─────────────────────────────────────────────────────┘
                              ↓
        ┌────────────────────────────────────┐
        │  Available on Maven Central          │
        │  - 2-3 hours propagation time        │
        │  - Searchable in Maven repositories  │
        │  - CI/CD ready for downstream users  │
        └────────────────────────────────────┘
```

### 1.2 Workflow Decision Tree

```
Event Type              → Workflow Triggered           → Actions
─────────────────────────────────────────────────────────────────
Pull Request Open       → CI (build + test)           → Comment on PR
Pull Request Updated    → CI (build + test)           → Update checks
Merge to Main           → Nothing (already passed CI)  → N/A
Create Tag (vX.Y.Z)     → Release (build + sign + pub)→ Publish
Manual Dispatch         → Release (for hotfixes)      → Publish

Timeline:
- PR CI: 3-5 minutes
- Release: 5-10 minutes total
  - Build & Sign: 2-3 min
  - Deploy to Staging: 1-2 min
  - Auto-promote to Release: 1 min
  - Maven Central sync: 2-3 hours
```

---

## 2. Prerequisites & Configuration

### 2.1 Maven Central (OSSRH) Setup

**One-time setup (do once, before first release)**:

#### Step 1: Create Sonatype Account
1. Sign up at https://issues.sonatype.org (Sonatype JIRA)
2. Create JIRA ticket requesting namespace (e.g., `com.github.yourusername` or `io.akka`)
3. Wait for approval (usually 1-2 days)

#### Step 2: Create GPG Key
```bash
# Generate GPG key (if you don't have one)
gpg --gen-key

# Follow prompts:
# - Real name: Your Name
# - Email: your-email@example.com
# - Passphrase: Strong password (you'll need this)

# List keys to get KEY_ID
gpg --list-keys

# Publish key to public keyserver
gpg --keyserver hkp://pool.sks-keyservers.net --send-keys YOUR_KEY_ID
```

#### Step 3: Export GPG Private Key
```bash
# Export private key (needed for GitHub Actions)
gpg --export-secret-keys YOUR_KEY_ID | base64 > gpg-secret.txt

# Display and copy the contents (very long string)
cat gpg-secret.txt
```

#### Step 4: Create GitHub Secrets
In your GitHub repository (Settings → Secrets and variables → Actions), add:

| Secret Name | Value | Source |
|-------------|-------|--------|
| `OSSRH_USERNAME` | Your Sonatype username | JIRA account |
| `OSSRH_TOKEN` | Your Sonatype token | JIRA Profile → User Token |
| `MAVEN_GPG_PRIVATE_KEY` | Base64-encoded GPG key | Output from `gpg-secret.txt` |
| `MAVEN_GPG_PASSPHRASE` | GPG key passphrase | Your passphrase from step 2 |

**How to get Sonatype Token**:
1. Log into https://oss.sonatype.org
2. Click your username (top right) → Profile
3. Select "User Token" from dropdown
4. Copy username and password (these are your token credentials)

### 2.2 Project Structure Setup

```
akka-openapi-maven-plugin/
├── .github/
│   └── workflows/
│       ├── ci.yml                    # PR validation workflow
│       ├── release.yml               # Release workflow
│       └── code-quality.yml          # Optional: SonarQube, SpotBugs
├── pom.xml                           # Main build config
├── src/
│   ├── main/java/
│   ├── test/java/
│   └── it/                           # Integration tests
├── README.md
├── CHANGELOG.md
├── LICENSE (Apache 2.0)
└── release/
    └── m2-settings.xml              # Maven settings for CI/CD
```

### 2.3 POM.xml Configuration for Maven Central

Add to your `pom.xml`:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  
  <!-- Maven Central Requirement: Use your approved namespace -->
  <groupId>io.akka</groupId>
  <artifactId>akka-openapi-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>maven-plugin</packaging>
  
  <name>Akka OpenAPI Maven Plugin</name>
  <description>Auto-generate OpenAPI 3.1 specs from Akka SDK annotations</description>
  <url>https://github.com/yourusername/akka-openapi-maven-plugin</url>
  
  <!-- License (required) -->
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  
  <!-- Developer info (required) -->
  <developers>
    <developer>
      <id>yourusername</id>
      <name>Your Name</name>
      <email>your-email@example.com</email>
      <organization>Your Organization</organization>
    </developer>
  </developers>
  
  <!-- SCM (required for Maven Central) -->
  <scm>
    <connection>scm:git:https://github.com/yourusername/akka-openapi-maven-plugin.git</connection>
    <developerConnection>scm:git:https://github.com/yourusername/akka-openapi-maven-plugin.git</developerConnection>
    <url>https://github.com/yourusername/akka-openapi-maven-plugin</url>
    <tag>HEAD</tag>
  </scm>
  
  <!-- Distribution management (Maven Central - OSSRH) -->
  <distributionManagement>
    <snapshotRepository>
      <id>ossrh</id>
      <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </snapshotRepository>
    <repository>
      <id>ossrh</id>
      <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
    </repository>
  </distributionManagement>
  
  <build>
    <pluginManagement>
      <plugins>
        <!-- Maven Release Plugin -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-release-plugin</artifactId>
          <version>3.0.1</version>
          <configuration>
            <tagNameFormat>v@{project.version}</tagNameFormat>
            <autoVersionSubmodules>true</autoVersionSubmodules>
            <scmCommentPrefix>[skip ci]</scmCommentPrefix>
          </configuration>
        </plugin>
        
        <!-- GPG Plugin (signs artifacts) -->
        <!-- Use version 3.2.0+ for Bouncy Castle support (no GPG binary needed) -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-gpg-plugin</artifactId>
          <version>3.2.4</version>
          <executions>
            <execution>
              <id>sign-artifacts</id>
              <phase>verify</phase>
              <goals>
                <goal>sign</goal>
              </goals>
              <configuration>
                <signer>bc</signer>
              </configuration>
            </execution>
          </executions>
        </plugin>
        
        <!-- Nexus Staging Plugin (auto-promote from staging to release) -->
        <plugin>
          <groupId>org.sonatype.plugins</groupId>
          <artifactId>nexus-staging-maven-plugin</artifactId>
          <version>1.6.13</version>
          <extensions>true</extensions>
          <configuration>
            <serverId>ossrh</serverId>
            <nexusUrl>https://oss.sonatype.org/</nexusUrl>
            <autoReleaseAfterClose>true</autoReleaseAfterClose>
          </configuration>
        </plugin>
        
        <!-- Source JAR Plugin -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-source-plugin</artifactId>
          <version>3.3.1</version>
          <executions>
            <execution>
              <id>attach-sources</id>
              <goals>
                <goal>jar-no-fork</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
        
        <!-- Javadoc Plugin (required for Maven Central) -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-javadoc-plugin</artifactId>
          <version>3.6.3</version>
          <executions>
            <execution>
              <id>attach-javadocs</id>
              <goals>
                <goal>jar</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
        
        <!-- Compiler Plugin -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.11.0</version>
          <configuration>
            <source>11</source>
            <target>11</target>
          </configuration>
        </plugin>
        
        <!-- Surefire (unit tests) -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>3.1.2</version>
        </plugin>
      </plugins>
    </pluginManagement>
    
    <plugins>
      <!-- Enable source, javadoc, and GPG plugins -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-source-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-javadoc-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-gpg-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.sonatype.plugins</groupId>
        <artifactId>nexus-staging-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## 3. GitHub Actions Workflows

### 3.1 CI Workflow (Pull Request Validation)

**File**: `.github/workflows/ci.yml`

```yaml
name: CI Build & Test

on:
  pull_request:
    branches: [ main, develop ]
  push:
    branches: [ main, develop ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    
    strategy:
      matrix:
        java-version: [ '11', '17', '21' ]
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for version detection
      
      - name: Set up JDK ${{ matrix.java-version }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java-version }}
          distribution: 'corretto'
          cache: maven
      
      - name: Build and test
        run: |
          mvn clean verify \
            --batch-mode \
            --no-transfer-progress \
            -DskipITs=false
      
      - name: Run integration tests
        run: |
          mvn verify \
            --batch-mode \
            --no-transfer-progress
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results-${{ matrix.java-version }}
          path: target/surefire-reports/
      
      - name: Publish test results
        if: always()
        uses: EnricoMi/publish-unit-test-result-action@v2
        with:
          files: 'target/surefire-reports/TEST-*.xml'
          check_name: Test Results (JDK ${{ matrix.java-version }})

  code-quality:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'corretto'
          cache: maven
      
      - name: Build for analysis
        run: |
          mvn clean compile \
            --batch-mode \
            --no-transfer-progress
      
      - name: Run SpotBugs
        run: |
          mvn spotbugs:check \
            --batch-mode \
            --no-transfer-progress || true
      
      - name: Run Checkstyle
        run: |
          mvn checkstyle:check \
            --batch-mode \
            --no-transfer-progress || true
      
      - name: Check dependencies for vulnerabilities
        run: |
          mvn dependency-check:check \
            --batch-mode \
            --no-transfer-progress || true

  coverage:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'corretto'
          cache: maven
      
      - name: Build with coverage
        run: |
          mvn clean verify \
            -Pcoverage \
            --batch-mode \
            --no-transfer-progress
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml
          flags: unittests
          fail_ci_if_error: false
```

### 3.2 Release Workflow (Maven Central Publication)

**File**: `.github/workflows/release.yml`

```yaml
name: Release to Maven Central

on:
  push:
    tags:
      - 'v*.*.*'  # Triggers on tags like v1.0.0, v1.2.3, etc.
  workflow_dispatch:
    inputs:
      version:
        description: 'Release version (optional, auto-detected if not provided)'
        required: false

jobs:
  validate:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.get-version.outputs.version }}
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Get version from tag or input
        id: get-version
        run: |
          if [ "${{ github.event_name }}" = "push" ]; then
            VERSION=${GITHUB_REF#refs/tags/v}
          else
            VERSION=${{ github.event.inputs.version }}
          fi
          echo "version=$VERSION" >> $GITHUB_OUTPUT
          echo "Releasing version: $VERSION"
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'corretto'
          cache: maven
      
      - name: Validate POM version matches tag
        run: |
          PROV_VERSION=${{ steps.get-version.outputs.version }}
          POM_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
          echo "Tag version: $PROV_VERSION"
          echo "POM version: $POM_VERSION"
          if [ "$PROV_VERSION" != "$POM_VERSION" ]; then
            echo "ERROR: POM version ($POM_VERSION) does not match tag ($PROV_VERSION)"
            exit 1
          fi
      
      - name: Quick compile check
        run: |
          mvn clean compile \
            --batch-mode \
            --no-transfer-progress

  build-and-sign:
    needs: validate
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Set up JDK 17 with GPG
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'corretto'
          cache: maven
          gpg-private-key: ${{ secrets.MAVEN_GPG_PRIVATE_KEY }}
          gpg-passphrase: ${{ secrets.MAVEN_GPG_PASSPHRASE }}
      
      - name: Build and sign artifacts
        env:
          MAVEN_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.OSSRH_TOKEN }}
          MAVEN_GPG_PASSPHRASE: ${{ secrets.MAVEN_GPG_PASSPHRASE }}
        run: |
          mvn clean verify \
            --batch-mode \
            --no-transfer-progress \
            -DskipTests
      
      - name: Deploy to Maven Central Staging
        env:
          MAVEN_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.OSSRH_TOKEN }}
          MAVEN_GPG_PASSPHRASE: ${{ secrets.MAVEN_GPG_PASSPHRASE }}
        run: |
          mvn deploy \
            --batch-mode \
            --no-transfer-progress \
            -DskipTests \
            -Psonatype-oss-release \
            -Dgpg.passphrase=${{ secrets.MAVEN_GPG_PASSPHRASE }}
      
      - name: Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: maven-artifacts
          path: target/*.jar

  create-release:
    needs: [ validate, build-and-sign ]
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          name: maven-artifacts
          path: artifacts/
      
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          tag_name: v${{ needs.validate.outputs.version }}
          name: Release v${{ needs.validate.outputs.version }}
          draft: false
          prerelease: false
          generate_release_notes: true
          files: |
            artifacts/*.jar
            artifacts/*.pom
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Release Notes
        run: |
          echo "✅ Release v${{ needs.validate.outputs.version }} published!"
          echo ""
          echo "🎯 Release Checklist:"
          echo "- ✅ Code signed with GPG"
          echo "- ✅ Deployed to Maven Central Staging"
          echo "- ✅ Auto-promoted to Release Repository"
          echo "- ✅ GitHub Release created"
          echo ""
          echo "📦 Maven Central Sync Timeline:"
          echo "- Immediate: Available in staging repository"
          echo "- 10-30 min: Synced to Maven Central"
          echo "- 2-3 hours: Searchable in Maven Central"
          echo ""
          echo "🔗 Maven Central URL:"
          echo "https://search.maven.org/artifact/io.akka/akka-openapi-maven-plugin/${{ needs.validate.outputs.version }}"
          echo ""
          echo "📚 Download instruction:"
          echo "<dependency>"
          echo "  <groupId>io.akka</groupId>"
          echo "  <artifactId>akka-openapi-maven-plugin</artifactId>"
          echo "  <version>${{ needs.validate.outputs.version }}</version>"
          echo "</dependency>"

  notify-slack:
    needs: [ validate, create-release ]
    runs-on: ubuntu-latest
    if: always()
    
    steps:
      - name: Send Slack notification (optional)
        uses: 8398a7/action-slack@v3
        if: always()
        with:
          status: ${{ job.status }}
          text: "akka-openapi-maven-plugin v${{ needs.validate.outputs.version }} released!"
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
          fields: repo,message,commit,author,action,eventName,ref,workflow
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

### 3.3 Optional: Code Quality Workflow (SonarQube)

**File**: `.github/workflows/sonarqube.yml`

```yaml
name: Code Quality (SonarQube)

on:
  pull_request:
    branches: [ main ]

jobs:
  sonarqube:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code with full history
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'corretto'
          cache: maven
      
      - name: Build and run tests
        run: |
          mvn clean verify \
            --batch-mode \
            --no-transfer-progress
      
      - name: SonarCloud Scan
        uses: SonarSource/sonarcloud-github-action@master
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        with:
          args: >
            -Dsonar.projectKey=akka-openapi-maven-plugin
            -Dsonar.organization=yourusername
            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

---

## 4. Release Process & Versioning

### 4.1 Semantic Versioning

Use **Semantic Versioning** (MAJOR.MINOR.PATCH):

```
MAJOR.MINOR.PATCH
├─ MAJOR: Breaking changes (rare for plugins)
├─ MINOR: New features (backward compatible)
└─ PATCH: Bug fixes, minor improvements

Examples:
- v1.0.0 → v1.0.1 (patch: bug fix)
- v1.0.1 → v1.1.0 (minor: new feature)
- v1.1.0 → v2.0.0 (major: breaking change)

Pre-release:
- v1.0.0-RC1 (release candidate)
- v1.0.0-BETA (beta)
- v1.0.0-ALPHA (alpha)
```

### 4.2 Release Preparation Checklist

**Before releasing, ensure**:

- [ ] All tests passing on main branch
- [ ] Code review completed (if required)
- [ ] CHANGELOG.md updated with new version and features
- [ ] Version number in pom.xml reflects release version (no -SNAPSHOT)
- [ ] JavaDoc is complete and accurate
- [ ] All dependencies are stable (no -SNAPSHOT deps)
- [ ] GitHub issues are tagged appropriately
- [ ] Release notes drafted

### 4.3 Manual Release Process

**Step 1: Update Version in pom.xml**
```bash
# Remove -SNAPSHOT suffix
# Before: <version>1.0.0-SNAPSHOT</version>
# After:  <version>1.0.0</version>

# Commit and push
git add pom.xml
git commit -m "chore: bump version to 1.0.0"
git push origin main
```

**Step 2: Create Git Tag**
```bash
# Create an annotated tag
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push tag to trigger release workflow
git push origin v1.0.0
```

**Step 3: Monitor Release Workflow**
- GitHub Actions automatically triggers
- Check Actions tab for workflow status
- Release takes 5-10 minutes
- GitHub Release created automatically

**Step 4: Update Version to Next Snapshot**
```bash
# After release is confirmed, bump to next snapshot version
# Before: <version>1.0.0</version>
# After:  <version>1.0.1-SNAPSHOT</version>

git add pom.xml
git commit -m "chore: prepare for next development iteration"
git push origin main
```

### 4.4 Automated Release (Manual Trigger)

Trigger release via GitHub Actions UI without code changes:

1. Go to GitHub repo → Actions
2. Select "Release to Maven Central"
3. Click "Run workflow"
4. Enter version number (optional)
5. Click "Run workflow"

---

## 5. Maven Central Publishing Details

### 5.1 Publishing Timeline & Status Checking

**Timeline**:
```
T+0:   Release workflow completes
T+10-30 min: Available in Maven Central
T+1-3 hours: Searchable and indexed
T+24 hours:  Propagated to mirrors worldwide
```

**Check release status**:

```bash
# Method 1: Maven Central Search
# https://search.maven.org/artifact/io.akka/akka-openapi-maven-plugin/1.0.0/

# Method 2: OSSRH Nexus
# https://oss.sonatype.org/#stagingRepositories
# Look for your staging repo, verify and close it (auto-promoted)

# Method 3: Direct download test
mvn dependency:get \
  -Dartifact=io.akka:akka-openapi-maven-plugin:1.0.0
```

### 5.2 Verification Checklist

After release, verify:

- [ ] GitHub Release created with tag
- [ ] Release notes auto-generated
- [ ] JAR signed with GPG
- [ ] POM signed with GPG
- [ ] Source JAR available
- [ ] Javadoc JAR available
- [ ] Available on Maven Central Search
- [ ] Searchable in IDE (Maven plugin browser)
- [ ] No build errors from downstream users

### 5.3 Troubleshooting Release Issues

**Problem**: "Invalid signature"
```
Solution:
1. Verify GPG secret key is correctly base64-encoded
2. Check passphrase matches
3. Ensure maven-gpg-plugin version 3.2.0+
```

**Problem**: "401 Unauthorized to OSSRH"
```
Solution:
1. Verify OSSRH_USERNAME and OSSRH_TOKEN in GitHub Secrets
2. Check token hasn't expired (regenerate in Sonatype if needed)
3. Ensure OSSRH credentials are correct (check profile.xml)
```

**Problem**: "Version already exists"
```
Solution:
1. Release was successful (double-check Maven Central Search)
2. Don't try to re-release same version
3. Bump version and create new release
```

**Problem**: "Staging repository failed validation"
```
Solution:
1. Check logs for missing artifact (source/javadoc/POM)
2. Ensure all required files signed
3. Verify no dependencies on SNAPSHOT versions
```

---

## 6. Local Development & Testing

### 6.1 Building Locally

```bash
# Standard build
mvn clean package

# Build with all checks
mvn clean verify -Pall-checks

# Build without tests (faster)
mvn clean package -DskipTests

# Build with coverage
mvn clean verify -Pcoverage
```

### 6.2 Local Release Testing

```bash
# Dry-run release (doesn't deploy, shows what would happen)
mvn release:prepare \
  -DdryRun=true \
  --batch-mode

# Clean up dry-run
mvn release:clean
```

### 6.3 Testing Against Local Maven Repo

```bash
# Install to local repo (~/.m2/repository)
mvn clean install

# Use in another project
# Add to test project's pom.xml:
<plugin>
  <groupId>io.akka</groupId>
  <artifactId>akka-openapi-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>generate</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

---

## 7. Post-Release Tasks

### 7.1 Announce Release

- [ ] Tweet/announce on social media
- [ ] Post in Akka community forums
- [ ] Update GitHub release with highlights
- [ ] Add to project website/blog
- [ ] Notify early adopters (if any)

### 7.2 Documentation Updates

- [ ] Update README with latest version
- [ ] Update getting-started guide if features changed
- [ ] Update CHANGELOG for next development cycle
- [ ] Update website/documentation site

### 7.3 Issue Triage

- [ ] Close issues resolved in release
- [ ] Tag new issues appropriately
- [ ] Prioritize backlog for next release

---

## 8. Security Best Practices

### 8.1 Secrets Management

**DO**:
- ✅ Store GPG key and credentials in GitHub Secrets
- ✅ Use unique strong passwords for GPG
- ✅ Rotate tokens periodically (Sonatype)
- ✅ Review GitHub Actions logs for leaks
- ✅ Use minimal-permission secrets

**DON'T**:
- ❌ Commit secrets to git
- ❌ Store plaintext keys in pom.xml
- ❌ Share secrets via email/chat
- ❌ Reuse passwords across systems
- ❌ Log sensitive values

### 8.2 GPG Key Management

```bash
# Export public key for distribution
gpg --armor --export YOUR_KEY_ID > pubkey.asc

# Backup private key securely
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key-backup.asc
# Store backup in secure location (encrypted disk, vault)

# Revoke key (if compromised)
gpg --gen-revoke YOUR_KEY_ID > revoke.asc
gpg --import revoke.asc
gpg --keyserver hkp://pool.sks-keyservers.net --send-keys YOUR_KEY_ID
```

### 8.3 Audit Trail

GitHub Actions provides complete audit trail:
- Who triggered release
- What changes were deployed
- Release date/time
- Logs available for 90 days

---

## 9. Monitoring & Alerting

### 9.1 Setting Up Alerts

Optional: Configure Slack notifications for release status:

```bash
# Get Slack webhook URL
# 1. Go to your Slack workspace
# 2. Create incoming webhook at api.slack.com/messaging/webhooks
# 3. Add to GitHub Secrets as SLACK_WEBHOOK

# Release workflow will notify on success/failure
```

### 9.2 Monitoring Downloads

Track plugin usage:
- Maven Central statistics: https://stats.maven.org/
- Search for: `akka-openapi-maven-plugin`
- Monitor GitHub Stars/Forks
- Track issue volume as proxy for adoption

---

## 10. Rollback & Hotfix Process

### 10.1 Quick Hotfix Release

If critical bug found immediately after release:

```bash
# Create hotfix branch
git checkout -b hotfix/v1.0.1
git checkout main

# Make critical fix
# Commit changes
git add .
git commit -m "fix: critical bug in schema generation"

# Create new release
git tag -a v1.0.1 -m "Hotfix: critical schema bug"
git push origin hotfix/v1.0.1 v1.0.1

# Merge back to main
git checkout main
git merge hotfix/v1.0.1
git push origin main

# Delete hotfix branch
git branch -d hotfix/v1.0.1
```

### 10.2 Yanking a Release

If release is fundamentally broken (rare):

1. Manual yank via OSSRH UI (Nexus)
2. Create new release with fixes
3. Document in release notes

---

## 11. Configuration Files & Templates

### 11.1 Release Settings XML

**File**: `release/m2-settings.xml` (for local override)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>${env.OSSRH_USERNAME}</username>
      <password>${env.OSSRH_TOKEN}</password>
    </server>
  </servers>
  
  <profiles>
    <profile>
      <id>sonatype-oss-release</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-source-plugin</artifactId>
            <executions>
              <execution>
                <id>attach-sources</id>
                <goals>
                  <goal>jar-no-fork</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
          
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-javadoc-plugin</artifactId>
            <executions>
              <execution>
                <id>attach-javadocs</id>
                <goals>
                  <goal>jar</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
          
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-gpg-plugin</artifactId>
            <executions>
              <execution>
                <id>sign-artifacts</id>
                <phase>verify</phase>
                <goals>
                  <goal>sign</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
</settings>
```

### 11.2 GitHub Release Template

**File**: `.github/RELEASE_TEMPLATE.md`

```markdown
## Version X.Y.Z - Release Date

### ✨ Features
- New feature A
- New feature B

### 🐛 Bug Fixes
- Fixed bug A (fixes #123)
- Fixed bug B (fixes #456)

### 📦 Dependency Updates
- Updated Jackson to 2.16.0
- Updated Maven plugins

### 🔒 Security
- Fixed XSS vulnerability in input validation (CVE-XXXX-XXXXX)

### 💥 Breaking Changes
- Removed deprecated method X
- Changed behavior of Y

### 🙏 Contributors
- @contributor1
- @contributor2

### 📥 Installation

```xml
<dependency>
  <groupId>io.akka</groupId>
  <artifactId>akka-openapi-maven-plugin</artifactId>
  <version>X.Y.Z</version>
</dependency>
```

### 📝 Changelog
See full changelog: [CHANGELOG.md](CHANGELOG.md)

### 🙋 Getting Help
- Documentation: [README.md](README.md)
- Issues: [GitHub Issues](../../issues)
- Discussions: [GitHub Discussions](../../discussions)
```

---

## 12. Performance & Metrics

### 12.1 Build Performance Targets

| Stage | Target | Actual |
|-------|--------|--------|
| CI (PR validation) | < 5 minutes | ~3-4 min |
| Build artifacts | < 2 minutes | ~1-2 min |
| GPG signing | < 1 minute | ~30-40 sec |
| Deploy to staging | < 2 minutes | ~1-2 min |
| Total release | < 10 minutes | ~5-7 min |

### 12.2 Release Metrics Dashboard

Track over time:
- Release frequency (target: monthly or quarterly)
- Time from merge to release (target: < 24 hours)
- Release success rate (target: 100%)
- Community adoption (stars, downloads)
- Issue resolution time

---

## 13. Maintenance & Upgrades

### 13.1 Quarterly Dependency Updates

```bash
# Check for updates
mvn versions:display-dependency-updates

# Update all plugins
mvn versions:update-properties

# Test thoroughly before releasing
mvn clean verify
```

### 13.2 GitHub Actions Runtime Updates

- Review GitHub Actions for deprecations
- Update action versions quarterly
- Monitor breaking changes in Java versions
- Test on new Java LTS versions (21, 25, etc.)

---

## 14. Disaster Recovery

### 14.1 Repository Backup

```bash
# Full clone backup
git clone --mirror https://github.com/yourusername/akka-openapi-maven-plugin.git

# Store in secure location (could be GitHub itself with multiple remotes)
```

### 14.2 GPG Key Recovery

```bash
# Restore from encrypted backup
# (Should be stored in secure vault, encrypted disk, etc.)

gpg --import private-key-backup.asc
gpg --trust-model always -ab <file>  # Verify key works
```

### 14.3 Maven Central Help

Contact Sonatype for:
- Version yanking (remove compromised release)
- Credential reset
- Account recovery
- Namespace transfer

---

## 15. Success Metrics (Post-Release)

Track these metrics for first 3 months after launch:

| Metric | Target | Notes |
|--------|--------|-------|
| Release automation success | 100% | All releases automated |
| Time to Maven Central | < 30 min | From workflow completion |
| CI/CD uptime | > 99% | Actions availability |
| Build failure rate | < 5% | Acceptable false positives |
| Release frequency | 1-2/month | Healthy cadence |
| Community adoption | 50+ stars | GitHub interest |
| Monthly downloads | 100+ | Maven Central usage |
| Issue response time | < 48 hours | Community engagement |

---

## Appendix: Quick Reference

### Common Commands

```bash
# Create release
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0

# Check release status
# Watch: https://github.com/yourusername/akka-openapi-maven-plugin/actions

# Search Maven Central
# https://search.maven.org/artifact/io.akka/akka-openapi-maven-plugin/

# Test locally
mvn clean verify -Pcoverage

# View GitHub Secrets
# Settings → Secrets and variables → Actions
```

### Important URLs

```
OSSRH Login: https://oss.sonatype.org
Maven Central Search: https://search.maven.org/
Sonatype JIRA: https://issues.sonatype.org
GPG Key Server: hkp://pool.sks-keyservers.net
GitHub Actions: https://github.com/{org}/{repo}/actions
```

### Support Resources

- Sonatype OSSRH Guide: https://central.sonatype.org/publish/publish-maven/
- GitHub Actions Docs: https://docs.github.com/actions
- Maven GPG Plugin: https://maven.apache.org/plugins/maven-gpg-plugin/
- Maven Release Plugin: https://maven.apache.org/plugins/maven-release-plugin/

---

**Document Version**: 1.0  
**Created**: January 2026  
**Last Updated**: January 2026  
**Status**: Ready for Implementation  
**Author**: CTO, OSO  

**Next Steps**:
1. ✅ Configure GitHub Secrets (OSSRH credentials, GPG key)
2. ✅ Create `.github/workflows/` directory
3. ✅ Add CI and release workflow YAML files
4. ✅ Update `pom.xml` with Maven Central configuration
5. ✅ Create first release tag (`git tag v1.0.0`)
6. ✅ Monitor release workflow execution
7. ✅ Verify publication on Maven Central
8. ✅ Announce release to community
