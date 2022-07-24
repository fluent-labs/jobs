pipeline {
    agent {
        kubernetes {
            yaml '''
        apiVersion: v1
        kind: Pod
        metadata:
          namespace: jobs
        spec:
          imagePullSecrets:
          - 'dockerhub'
          volumes:
          - name: sbt-cache
            persistentVolumeClaim:
              claimName: sbt-cache
          containers:
          - name: sbt
            image: lkjaero/jenkins-runners:sbt
            command:
            - sleep
            args:
            - 99d
            volumeMounts:
            - name: sbt-cache
              mountPath: /root/.cache/coursier/v1/
      '''
        }
    }

    stages {
        stage("Build the jar") {
            steps {
                container('sbt') {
                    sh 'sbt assembly'
                    sh 'sbt test'
                }
            }
        }
    }
}