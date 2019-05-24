
In order to use GCP 'Cloud run', you must first activate it in your gcp account.
After enabling it, navigate to https://console.cloud.google.com/marketplace/details/google-cloud-platform/cloud-run

# Deploy the docker-function on gcp 'Cloud run'
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
Use https://ngrok.com/ to debug calls on local service
