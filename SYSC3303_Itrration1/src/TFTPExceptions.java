import java.io.IOException;
 
public class TFTPExceptions {
 
    //ERROR CODE 1
    //called by the client when it performs a local read on a file that 
    //that doesn't exist (this happens when performing a write request and 
    //should be handle locally)
    //called by the server when it performs a local read on a file that 
    //that doesn't exist (this happens when performing a read request)
    public class FileNotFoundException extends Exception {
        public FileNotFoundException(String message) {
            super(message);
        }
    }
 
    //ERROR CODE 2
    //TO BE DETERMINED
    public class AccessViolationException extends Exception {
        public AccessViolationException(String message) {
            super(message);
        }
    }
 
    //ERROR CODE 3
    //Called by the client when disk is full locally, same thing on the server side
    public class DiskFullException extends Exception {
        public DiskFullException(String message) {
            super(message);
        }
    }
 
    //ERROR CODE 4
    //exception which gets thrown by the server when an invalid request is detected
    public class InvalidTFTPRequestException extends Exception {
        public InvalidTFTPRequestException(String message) {
            super(message);
        }
    }
 
    //ERROR CODE 4
    //called by the client in a read request or called by the server in a write
    public class InvalidTFTPDataException extends Exception {
        public InvalidTFTPDataException(String message) {
            super(message);
        }
    }
 
    //ERROR CODE 4
    //called by the client in a read request or called by the server in a write
    public class InvalidTFTPAckException extends Exception {
        public InvalidTFTPAckException(String message) {
            super(message);
        }
    }
 
    //ERROR CODE 5
    //Called by the client or server upon receiving a packet from an unknown destination
    public class UnknownTransferID extends Exception {
        public UnknownTransferID(String message) {
            super(message);
        }
    }
 
    //ERROR CODE 6
    //called by the client when it tries to read a file from the server which 
    //already exists locally
    //called by the server when the client tries to write a file to the server
    //which already exists on the server side
    public class FileAlreadyExistsException extends Exception {
        public FileAlreadyExistsException(String message) {
            super(message);
        }
    }
 
}