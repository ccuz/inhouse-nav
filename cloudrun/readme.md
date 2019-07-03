# Dialogflow webhook on gcp Cloudrun

Dialogflow Webhook implemented in Java with Akka Http and "action on google" java sdk

## Dialogflow agent setup
Create a new Dialogflow agent by importing the 'src/main/dialogflow/inhouse-nav-cloudrun.zip' agent setup
- During the import, create a new GCP project for that agent
- Change the cloudbuild.yaml if needed and create a GCP Cloud Build for that project
  - Per default, the Cloudrun function is public due to '--allow-unauthenticated' usage, to facilitate debugging
- Trigger a Cloudbuild and inspect your function under 'Cloudrun'
- In Dialogflow, configure the agent 'Fulfillment' webhook URL to point on the Cloudrun function endpoint (i.e. https://{cloudrun-service-id}.a.run.app)
- Within the Dialogflow console, click on 'See how it works in Google Assistant' to debug within the simulator


## Deploy the docker-function on gcp 'Cloud run'

In order to use GCP 'Cloud run', you must first activate it in your gcp account.
After enabling it, navigate to https://console.cloud.google.com/marketplace/details/google-cloud-platform/cloud-run

gcloud builds submit --tag gcr.io/[PROJECT-ID]/dialogflowcloudrun

Upon success, you will see a SUCCESS message containing the image name (gcr.io/[PROJECT-ID]/dialogflowcloudrun). The image is stored in Container Registry and can be re-used if desired.

gcloud run deploy <service-name> --image gcr.io/[PROJECT-ID]/dialogflowcloudrun

or
gcloud builds submit --tag gcr.io/[PROJECT-ID]/[IMAGE]

gcloud beta run deploy --image gcr.io/[PROJECT-ID]/dialogflowcloudrun \
      --set-env-vars="JAVA_TOOL_OPTIONS=-XX:MaxRAM=256m"

# GCP Dialogflow interface
See https://github.com/googleapis/googleapis/tree/master/google/cloud/dialogflow/v2beta1 for protocol buffer definitions

# To test your bot
https://console.actions.google.com/project/[projectName]/simulator

#clean-up
Go to https://console.cloud.google.com/run?project=[projectName] to delete your cloud-run function
Go to https://console.dialogflow.com/api-client, select your agent to delete it (scroll till the bottom of the page)

#local testing
Start the 'DialogFlowFunction' main locally and a REST client to send POST request against http://localhost:8080/ with
with the test body json.

You can also use https://ngrok.com/ to debug calls against your local service by registering your dialogflow webhook url 
as configured in ngrok.

To test the docker image of the cloudrun function
- gradle build
- docker build -f Dockerfile.graalvm ./
- docker run yourDockerImageId
