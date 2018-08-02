Music App
==============

ABDN project.
==============


About project:

* App work with bound service - here I have a lot of troubles, but finally start working. 

* Notification to the user that provide action buttons Play/Stop, Next, Previous. Song title too.

* App have two activities. First is main player activity where user have all control over player.

* Next one is List Activity, where is List View with List Adapter implementing ViewHolder.

* Simple buttons animations, and little stupid playing animation that have to be fixed.

* when app is first install I run background code to upload some very nice free music files on Public External Device Storage.

  This code is run only once on install. For that emulator will need some space to install app.


NOTE: I come into problem with two thinks:

        1) On First start something happen with Notification and if press play buttons nothing will happen. Activity need refresh.
            
            for that please on first run after install go to list activity and play some file from there, or press hardware back button.
            
            When this is done, all is working. After that when app is run all is working good.




