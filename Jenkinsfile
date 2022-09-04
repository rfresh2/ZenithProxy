String getDiscordMessage() {
    def msg = "**Status:** " + currentBuild.currentResult.toLowerCase() + "\n**Branch:** ${BRANCH_NAME}\n**Changes:**"
    if (!currentBuild.changeSets.isEmpty()) {
        currentBuild.changeSets.first().getLogs().any {
            def line = "\n- `" + it.getCommitId().substring(0, 8) + "` *" + it.getComment().split("\n")[0].replaceAll('(?<!\\\\)([_*~`])', '\\\\$1') + "*"
            if (msg.length() + line.length() <= 1500)   {
                msg += line
                return
            } else {
                return true
            }
        }
    } else {
        msg += "\n- no changes"
    }

    msg += "\n**Artifacts:**"
    currentBuild.rawBuild.getArtifacts().any {
        def line = "\n- [" + it.getDisplayPath() + "](" + env.BUILD_URL + "artifact/" + it.getHref() + ")"
        if (msg.length() + line.length() <= 2000)   {
            msg += line
            return
        } else {
            return true
        }
    }
    return msg
}



pipeline {
    agent any
    tools {
        git "Default"
        jdk "jdk8"
    }
    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '5'))
    }
    stages {
        stage("Build") {
            steps {
                sh "./gradlew build -x publish --no-daemon"
            }
            post {
                success {
                    sh "bash ./add_jar_suffix.sh " + sh(script: "git log -n 1 --pretty=format:'%H'", returnStdout: true).substring(0, 8) + "-" + env.BRANCH_NAME.replaceAll("[^a-zA-Z0-9.]", "_")
                    archiveArtifacts artifacts: "build/libs/*.jar", fingerprint: true
                }
            }
        }
        stage("Publish") {
            when {
                anyOf {
                    branch "master"
                    branch "development"
                }
            }
            steps {
                sh "./gradlew publish -x publishToMavenLocal --no-daemon"
            }
        }
    }

    post {
        always {
            deleteDir()

            withCredentials([string(credentialsId: "daporkchop_discord_webhook", variable: "discordWebhook")]) {
                discordSend thumbnail: "https://cdn.discordapp.com/attachments/431945309011050496/703921035648565318/face-104f192a0f3e41e3b574919cc931559a.png",
                        result: currentBuild.currentResult,
                        description: getDiscordMessage(),
                        link: env.BUILD_URL,
                        title: "Proxy/${BRANCH_NAME} #${BUILD_NUMBER}",
                        webhookURL: "${discordWebhook}"
            }
        }
    }
}
