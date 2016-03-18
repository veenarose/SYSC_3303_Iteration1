SYSC 3303 
---------------
Iteration 2
-------------


Team 5
----------

Niresh Wewala  100917506  (Worked on Timing Diagram and ErrorSimulator)
Damjan Markosa 100823871 (Worked on Client)
Shray Pandey   100891947  (Worked on Server and ProfileData)
Veena Mathews  100909945 (Worked on UCM UML ErrorSimulator)


Instructions to Run the code: 
--------------------------------------
Run the Server class
Run the error simulator for simulating errors, pick any from 1 to 10.
		Then follow instructions for the corresponding error selected
Run the client class
	Choose any from: 
		Read or write request 
		Type in “octet” or “netascii”
		For Read, enter the name of the text file (from the ServerData folder)
		For Write, enter the name of the text file (from the ClientData folder)
		Wait to receive DATA or ACK packets

**********************************************************************************
Files Included : 
**********************************************************************************
Folders:
-------------
ClientData
ServerData
Iteration3_Diagrams

SRC Files:
--------------
Server.java
Client.java
ErrorSimulator.java
IOManager.java
PacketManager.java
ProfileData.java

UML Class Diagram Files:
-----------------------------------
Server_UML.png
Client_UML.png
ErrorSimulator_UML.png
IOManager_UML.png
PacketManager_UML.png
ProfileData_UML.png


UCM Diagram Files: 
---------------------------
Iteration1_ReadRequest_UCM.png
Iteration1_FileTransferWIthoutErrors_UCM.png


Timing Diagram Files:
-----------------------------
Iteration2_ErrorSim (Timing diagrams)
Delayed_ACK_1
Delayed_Packet_ReadRequest
DelayedACK0
Lost_ACK_WRQ
LostData_WRQ