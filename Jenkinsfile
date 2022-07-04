#!/usr/bin/env groovy

pipeline {
    agent {
        label 'slave-group-normal'
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '100', daysToKeepStr: '61'))
    }

    stages {
        stage('Prepare') {
            steps {
                // Show the agent name in the build list
                script {
                    // The manager variable requires the Groovy Postbuild plugin
                    manager.addShortText(env.NODE_NAME, "grey", "", "0px", "")
                }

                // Workaround for JENKINS-47230
                script {
                    env.MAVEN_HOME = tool('Maven')
                    env.MAVEN_OPTS = "-Xmx1g -XX:+HeapDumpOnOutOfMemoryError"
                    env.JAVA_HOME = tool('JDK 11')
                }
                sh 'cleanup.sh'
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                configFileProvider([configFile(fileId: 'maven-settings-with-deploy-snapshot', variable: 'MAVEN_SETTINGS')]) {
                    sh "$MAVEN_HOME/bin/mvn clean install -B -V -e -s $MAVEN_SETTINGS -DskipTests"
                }
            }
        }

        stage('JDK 11 Tests') {
            steps {
                // cleanup old xml
                sh 'find . -type d -name "*-reports*" -prune -exec rm -rf {} \\;'
                script {
                    env.JAVA_HOME = tool('JDK 11')
                }
                configFileProvider([configFile(fileId: 'maven-settings-with-deploy-snapshot', variable: 'MAVEN_SETTINGS')]) {
                    sh "$MAVEN_HOME/bin/mvn verify -B -V -e -s $MAVEN_SETTINGS -Dmaven.test.failure.ignore=true -Dansi.strip=true"
                }
                junit testResults: '**/target/*-reports*/**/TEST-*.xml', healthScaleFactor: 100, allowEmptyResults: true

                // Workaround for SUREFIRE-1426: Fail the build if there a fork crashed
                script {
                    if (manager.logContains("org.apache.maven.surefire.booter.SurefireBooterForkException:.*")) {
                        echo "Fork error found"
                        manager.buildFailure()
                    }
                }
            }
        }
        stage('JDK 17 Tests') {
            steps {
                // cleanup old xml
                sh 'find . -type d -name "*-reports*" -prune -exec rm -rf {} \\;'
                script {
                    env.JAVA_HOME = tool('JDK 17')
                }
                configFileProvider([configFile(fileId: 'maven-settings-with-deploy-snapshot', variable: 'MAVEN_SETTINGS')]) {
                    sh "$MAVEN_HOME/bin/mvn verify -B -V -e -s $MAVEN_SETTINGS -Dmaven.test.failure.ignore=true -Dansi.strip=true"
                }
                junit testResults: '**/target/*-reports*/**/TEST-*.xml', healthScaleFactor: 100, allowEmptyResults: true

                // Workaround for SUREFIRE-1426: Fail the build if there a fork crashed
                script {
                    if (manager.logContains("org.apache.maven.surefire.booter.SurefireBooterForkException:.*")) {
                        echo "Fork error found"
                        manager.buildFailure()
                    }
                }
            }
        }
    }

    post {
        cleanup {
            // Remove all untracked files, ignoring .gitignore
            sh 'git clean -qfdx || echo "git clean failed, exit code $?"'

            // Remove all created SNAPSHOT artifacts to ensure a clean build on every run
            sh 'find ~/.m2/repository -type d -name "*-SNAPSHOT" -prune -exec rm -rf {} \\;'
        }
    }
}
