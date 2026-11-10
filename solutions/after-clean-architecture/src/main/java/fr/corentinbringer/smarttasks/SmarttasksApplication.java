package fr.corentinbringer.smarttasks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
// Sérialise les Page<T> en { content, page: { size, number, totalElements, totalPages } } (format stable)
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class SmarttasksApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmarttasksApplication.class, args);
	}

}
