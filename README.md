# phonegapbuild-plugin
Jenkins plugin: phonegap build integration

This is a Phonegap Build (see http://build.phonegap.com/) integration for Jenkins.

## How does it work?

Phonegap Build provides some API to access its methods. So, in order to use the plugin, you need to log in there and create a project. You need to make sure it will compile successfully.

## Plugin configuration

* Application Id. See "App ID" in the phonegap build.
* Authentication Token. Click "Edit account" in the [Phonegap Build](https://build.phonegap.com/people/edit), click "Client applications", get the token from the "Authentication Tokens" field.
* Application name. It's the app name on the Project screen.
