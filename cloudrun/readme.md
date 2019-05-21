
In order to use GCP 'Cloud run', you must first activate it in your gcp account.
After enabling it, navigate to https://console.cloud.google.com/marketplace/details/google-cloud-platform/cloud-run

# Deploy the docker-function on gcp 'Cloud run'
gcloud run deploy <service-name> --image <image_name>