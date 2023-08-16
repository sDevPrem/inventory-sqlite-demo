# Inventory

It is an android java application project that uses SQlite database to store data. This app was given as a practice project at the end of Udacity Course - Android Basics: Data Storage.

## Screenshot

| ![screenshot_home](https://github.com/sDevPrem/inventory-sqlite-demo/assets/130966261/4bcf1153-0184-40af-8569-9e9003187452) | ![screenshot_details](https://github.com/sDevPrem/inventory-sqlite-demo/assets/130966261/2a1c845e-1e97-429c-a558-30a7836130e9) | 
|-----------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|


The app has two activity:

1. First one shows a list of inventory. Each Item has a sale button which reduces its quantity by one.
2. Second one is a details activity which shows more details about the selected product and allow the user to edit its details. Each inventory item contains:
   1. Image of the inventory
   2. Product name
   3. Price
   4. Quantity
   5. Description
   6. Supplier Phone Number
   7. Supplier Email

User can contact supplier by clicking on a floating action button at the bottom right corner with a phone icon by using the supplier contact details.

## Build With

1. Android Content Provider to do CRUD operation with the database.
2. Loaders to query the db and load data.
3. Cursor Adapter to show a list of item in a list view by reading it from the cursor.

It doesn't use any third party library.
