# Salz-Transport-REST

This is a small REST-API for public transport data. It uses the great library public-transport-enabler from Andreas Schildbach: [https://github.com/schildbach/public-transport-enabler]()

## Needed AWS roles:
- `codebuild-salz-transport-service-role` 
- `cloudformation-lambda-execution-role`

To setup these roles I refer to the AWS docs.

## CodePipleine

- Go to [https://eu-west-1.console.aws.amazon.com/codepipeline/]()
- Set a name for the pipeline `Salz-Transport-Ireland`
- Source provider is `GitHub` and Repository `hwuerz/salz-transport`. The branch is `master`
- Build Provider is `AWS CodeBuild` and `Create a new build project`
  - Project name `Salz-Transport-Ireland`
  - Environment image `Use an image managed by AWS CodeBuild`
  - Operating System `Ubuntu`
  - Runtime `Java`
  - Version `openjdk-8`
  - Build specification `Use the buildspec.yml in the source code root directory`
    (This will be the file in this repository)
  - Cache-Type `No cache`
  - AWS CodeBuild service role `Choose an existing service role from your account` and the role `codebuild-salz-transport-service-role`
  - VPC `No VPC`
  - Click `Save build project`
- Click `Next Step`
- Deployment-Provider `AWS Cloud Formation`
  - Action mode `Create or update a stack`
  - Stack name `Salz-Transport-Ireland`
  - Template file `cloud-formation-template.json` 
    (This will be the file in this repository)
  - Configuration file `[empty]`
  - Capabilities `CAPABILITY_IAM`
  - Role name `cloudformation-lambda-execution-role`
  - Click `Next step`
- AWS Service role
  - Role name `AWS-CodePipeline-Service`
  - Click `Next step`
- Click `Create pipeline`
- Edit the new pipeline
  - Edit Staging -> Salz-Transport-Ireland
  - Expand Advanced
  - Parameter overrides
    ```json
    {
      "BucketName" : { "Fn::GetArtifactAtt" : ["MyAppBuild", "BucketName"]},
      "ObjectKey" : { "Fn::GetArtifactAtt" : ["MyAppBuild", "ObjectKey"]}
    }
    ```

The pipeline is now available and a build can be performed.

## Lifecycle management in S3
- Go to [s3.console.aws.amazon.com]()
- Open the bucket for the new pipeline (something like `codepipeline-eu-west-1-dddddddddddd`)
- Click `Management`
- Click `Add lifecycle rule`
  - Rule name `Clean-up-rule`
  - Add filter to limit scope to prefix/tags `[Empty]`
  - Click `Next`
- Transitions
  - All fields `[unchecked]`
  - Click `Next`
- Expiration
  - Configure expiration check `Current version` and `Previous versions`
  - Set minimum length of `1` days for both
  - Click `Next`
- Click `Save`

## Required Roles

### codebuild-salz-transport-service-role 
### cloudformation-lambda-execution-role
### myRole for the lambda function 

To setup these roles I refer to the AWS docs.

