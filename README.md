# Installation and run
## Notice
* Compatibility tested for Windows 10 64bits only.
* Keep the folder structure or else you will need to edit the init.bat file manually.
* Requires jdk1.8.0_144 or higher.
## Start
* To start the Navigator - Start cmd, navigate to ``[pulled folder]/src``, run the following command:
	java MasterServer [your desired port]
After the MasterServer/Navigator is successfully initialized, 3 Subserver will open and ready to connected to. If any of them die they will be automatically revived after 4 seconds.
	
* To start the Client - Navigate to ``[pulled folder]/src``, run the following command:
	java Client [MasterServer's port]
	
# List of Client's command
** gettime@@@: This command get the subserver living time from when it is initialized.
** calculate@@@__param string__: This command return the character with highest occurence frequency.
** stop@@@: This command stop the subserver currently connect to the client, the subserver won't accept further connection, but still working for all currently connected client and only exit when all remaining clients is disconnected.
