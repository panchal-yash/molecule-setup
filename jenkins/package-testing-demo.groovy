    pipeline {
    agent {
        label 'min-bookworm-x64'
    }
    options {
        withCredentials(moleculePdpsJenkinsCreds())
    }

        stages {
            stage('Set Build Name'){
                steps {
                    script {
                        currentBuild.displayName = "${env.BUILD_NUMBER}-mysql-server-installation-test"
                    }
                }
            }

            stage('Checkout') {
                steps {
                    deleteDir()
                    git poll: false, branch: "main", url: "https://github.com/panchal-yash/molecule-setup.git"
                }
            }

            stage('Prepare') {
                steps {
                    script {
                        installMolecule()
                    }
                }
            }
            stage('RUN TESTS') {
                        steps {
                            script {
                                
                                moleculeParallelTest(OperatingSystems(), "molecule/ps/")
                                
                            }
                        }
            }

        }
    }

def moleculePdpsJenkinsCreds() {
  return [sshUserPrivateKey(credentialsId: 'MOLECULE_AWS_PRIVATE_KEY', keyFileVariable: 'MOLECULE_AWS_PRIVATE_KEY', passphraseVariable: '', usernameVariable: ''),
         [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: '5d78d9c7-2188-4b16-8e31-4d5782c6ceaa', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]
}


def OperatingSystems() {
    return ["ubuntu-noble","ubuntu-jammy","debian-12"]
}


def moleculeParallelTest(operatingSystems, moleculeDir) {
    tests = [:]
    operatingSystems.each { os ->
        tests["${os}"] =  {
            stage("${os}") {
                sh """
                    . virtenv/bin/activate
                    cd ${moleculeDir}
                    molecule test -s ${os}
                """
            }
        }
    }
    parallel tests
}

def installMolecule() {
        sh """
            sudo apt update -y
            sudo apt install -y python3 python3-pip python3-dev python3-venv
            python3 -m venv virtenv
            . virtenv/bin/activate
            python3 --version
            python3 -m pip install --upgrade pip
            python3 -m pip install --upgrade setuptools
            python3 -m pip install --upgrade setuptools-rust
            python3 -m pip install --upgrade PyYaml==5.3.1 molecule==3.3.0 testinfra pytest molecule-ec2==0.3 molecule[ansible] "ansible<10.0.0" "ansible-lint>=5.1.1,<6.0.0" boto3 boto
        """
}


