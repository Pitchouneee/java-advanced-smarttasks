package fr.corentinbringer.smarttasks.project.application.port.out;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStoragePort {

    String uploadFile(MultipartFile file) throws Exception;

    InputStream downloadFile(String objectKey) throws Exception;
}
