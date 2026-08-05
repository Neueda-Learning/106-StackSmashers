pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        COMPOSE_PROJECT_NAME = 'tms'
        COMPOSE_CMD_FILE = '.compose_cmd'
        ENV_FILE = '.env'
        WORK_DIR = 'repo'
        REPO_URL = 'https://github.com/Neueda-Learning/106-StackSmashers.git'
        REPO_BRANCH = 'main'
        REPO_CREDENTIALS_ID = ''
    }

    stages {
        stage('Checkout Source') {
            steps {
                script {
                    def repoUrl = env.REPO_URL?.trim()
                    def branch = env.REPO_BRANCH?.trim()
                    def credentialsId = env.REPO_CREDENTIALS_ID?.trim()

                    if (!repoUrl) {
                        error('REPO_URL is required.')
                    }

                    if (!branch) {
                        error('REPO_BRANCH is required.')
                    }

                    dir(env.WORK_DIR) {
                        deleteDir()

                        if (credentialsId) {
                            git branch: branch, credentialsId: credentialsId, url: repoUrl
                        } else {
                            git branch: branch, url: repoUrl
                        }
                    }
                }
            }
        }

        stage('Validate Agent Tooling') {
            steps {
                script {
                    if (!isUnix()) {
                        error('This pipeline requires a Linux Jenkins agent with git, curl, docker, and either docker compose or docker-compose installed.')
                    }

                    sh 'git --version'
                    sh 'docker --version'
                    sh 'curl --version'

                    def composeCmd = sh(
                        script: '''
if docker compose version >/dev/null 2>&1; then
    echo "docker compose"
elif docker-compose version >/dev/null 2>&1; then
    echo "docker-compose"
fi
''',
                        returnStdout: true
                    ).trim()

                    if (!composeCmd) {
                        error('Neither docker compose nor docker-compose is available on this Jenkins agent.')
                    }

                    writeFile file: env.COMPOSE_CMD_FILE, text: composeCmd + "\n"
                    sh "${composeCmd} version"
                }
            }
        }

        stage('Build Backend') {
            steps {
                dir("${env.WORK_DIR}/backend") {
                    sh 'chmod +x mvnw && ./mvnw -B clean package -DskipTests'
                }
            }
        }

        stage('Validate Frontend') {
            steps {
                dir("${env.WORK_DIR}/frontend") {
                    sh '''
if command -v npm >/dev/null 2>&1; then
    npm ci
    npm run build
else
    echo "npm is not installed on this Jenkins agent. Skipping local frontend validation; frontend will be built by Docker during deployment."
fi
'''
                }
            }
        }

        stage('Prepare Deployment Env') {
            steps {
                script {
                    def envContent = """
MYSQL_ROOT_PASSWORD=${env.MYSQL_ROOT_PASSWORD ?: 'n3u3da!'}
MYSQL_DATABASE=${env.MYSQL_DATABASE ?: 'tms_db'}
MYSQL_USER=${env.MYSQL_USER ?: 'tms_user'}
MYSQL_PASSWORD=${env.MYSQL_PASSWORD ?: 'tms_password'}
JWT_SECRET=${env.JWT_SECRET ?: 'd83f5e2a7c1b94d6e8f0a2b4c6d8e0f2a4b6c8d0e2f4a6b8c0d2e4f6a8b0c2d4'}
""".trim() + "\n"

                    writeFile file: "${env.WORK_DIR}/${env.ENV_FILE}", text: envContent
                }
            }
        }

        stage('Deploy With Docker Compose') {
            steps {
                script {
                    def composeCmd = readFile(env.COMPOSE_CMD_FILE).trim()
                    dir(env.WORK_DIR) {
                        // Ensure backend does not collide with Jenkins on host port 8080.
                        sh "sed -i 's/\"8080:8080\"/\"8081:8080\"/g' docker-compose.yml"
                        sh "${composeCmd} --env-file .env pull || true"
                        sh "${composeCmd} --env-file .env up -d --build --remove-orphans"
                        sh "${composeCmd} --env-file .env ps"
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def composeCmd = readFile(env.COMPOSE_CMD_FILE).trim()
                    dir(env.WORK_DIR) {
                        sh "${composeCmd} --env-file .env ps"
                    }
                    sh 'curl -fsS http://localhost:8081/api/actuator/health'
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment pipeline completed successfully.'
        }
        failure {
            echo 'Deployment pipeline failed. Check stage logs above.'
        }
        cleanup {
            script {
                sh 'rm -f .compose_cmd repo/.env'
            }
        }
    }
}
