# Printful - Shopware - Product - Sync
The tool is used to synchronize products from a Printful store into a Shopware 6 shop.

### Version
Tested with Shopware 6.4.3.0

### Create application.properties
Go to /src/resources.
There is already a sample file (application.properties-example) available. Please 
copy it to application.properties. Adjust the configuration to your needs.

### Printful preparation
1. Create a "manual store" in your printful account.
2. Enable API Access for your new manual store (Settings->Store->API):
![img.png](doc/printful_settings_api.png)
3. add the API key to your application.properties (sync.printful.apiKey)
4. add products to your manual store.


### Shopware preparation
1. Go to Properties->System->Integration
2. Create a new integration (name: printful, administrator: on) and add id and secret to your application.properties