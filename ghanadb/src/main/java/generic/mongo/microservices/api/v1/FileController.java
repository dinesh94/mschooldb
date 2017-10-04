/**
 * 
 */
package generic.mongo.microservices.api.v1;

import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import generic.mongo.microservices.model.RequestObject;
import generic.mongo.microservices.util.CommonUtil;
import generic.mongo.microservices.util.ImageUtil;
import generic.mongo.microservices.util.ImageWatermark;
import generic.mongo.microservices.util.LogUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author Dinesh
 *
 */
@RestController
@RequestMapping("api/v1/dbs/{db}/{collection}")
public class FileController {

	private static final Logger LOGGER = LogUtils.loggerForThisClass();
	
	@Resource
	private ResourceLoader resourceLoader;

	@Resource
	MongoClient mongoClient;

	@Value("${kandapohe.watermarkText}")
	private String watermarkText;

	/**
	 * @throws IOException
	 * 
	 **/
	@RequestMapping(method = { RequestMethod.GET }, value = "/files/{id}")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	//headers = "Accept=image/jpeg, image/jpg, image/png, image/gif", 
	public HttpEntity<byte[]> getFile(
			@ApiIgnore RequestObject request,
			@PathVariable("id") String objectId) throws IOException {

		@SuppressWarnings("deprecation")
		GridFS gfsPhoto = new GridFS(mongoClient.getDB(request.getDbName()), request.getCollectionName());

		BasicDBObject query = new BasicDBObject();
		query.put("_id", objectId);
		GridFSDBFile imageForOutput = gfsPhoto.findOne(query);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		imageForOutput.writeTo(bos);

		HttpHeaders header = new HttpHeaders();
		header.set("Content-Disposition", "attachment; filename=" + imageForOutput.getFilename());
		header.setContentLength(bos.size());

		return new HttpEntity<byte[]>(bos.toByteArray(), header);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/savefileOnDisk")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public FileUploadResponse savefileOnDisk(
			@ApiIgnore RequestObject request,
			@RequestParam("imageTypeOrFolderName") String imageTypeOrFolderName,
			@RequestParam("file") MultipartFile fileData) throws Exception {

		String fileID = null;
		FileUploadResponse fileUploadResponse = new FileUploadResponse();
		if (!fileData.isEmpty()) {
			try {
				try {
					final String originalFilename = fileData.getOriginalFilename();
					File newfile = ImageWatermark.addTextWatermark(watermarkText, fileData.getInputStream());
					
					File resized = ImageUtil.createThumbnailByImageScalr(newfile, "jpg", 600, 600);

					if (fileData.isEmpty()) {
						throw new Exception("Failed to store empty file " + originalFilename);
					}
					//String path = System.getProperty("user.home") + File.separator + "img";
					String path = "/usr/share/nginx/html/" + File.separator + imageTypeOrFolderName;
					LOGGER.debug("FileController.savefileOnDisk() path = " + path);
					File directory = new File(path);
					if (!directory.exists()) {
						directory.mkdir();
					}

					fileID = CommonUtil.getId();
					String fileName = fileID.toString() + ".png";
					String fullPath = path + File.separator + fileName;
					File customDir = new File(fullPath);
					FileUtils.copyFile(resized, customDir);

					fileUploadResponse.setFileName(fileName);
					fileUploadResponse.setFullPath(fullPath);
					fileUploadResponse.setFileId(fileID);
				}
				catch (IOException ex) {
					System.err.println(ex);
				}

			}
			catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
		return fileUploadResponse;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/savefile")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public FileUploadResponse handleFileUpload(
			@ApiIgnore RequestObject request,
			@RequestParam("file") MultipartFile fileData) {

		String fileID = null;
		FileUploadResponse fileUploadResponse = new FileUploadResponse();
		if (!fileData.isEmpty()) {
			try {
				@SuppressWarnings("deprecation")
				GridFS gfsPhoto = new GridFS(mongoClient.getDB(request.getDbName()), request.getCollectionName());

				try {
					final String originalFilename = fileData.getOriginalFilename();
					File newfile = ImageWatermark.addTextWatermark(watermarkText, fileData.getInputStream());
					GridFSInputFile gfsFile = gfsPhoto.createFile(newfile);

					fileID = CommonUtil.getId();
					fileUploadResponse.setFileId(fileID);

					gfsFile.setFilename(originalFilename);
					gfsFile.setId(fileID);

					gfsFile.save();
				}
				catch (IOException ex) {
					System.err.println(ex);
				}

			}
			catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
		return fileUploadResponse;
	}

	class FileUploadResponse {
		private String fileId = "";

		private String fileName = "";

		private String fullPath = "";

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getFullPath() {
			return fullPath;
		}

		public void setFullPath(String fullPath) {
			this.fullPath = fullPath;
		}

		public String getFileId() {
			return fileId;
		}

		public void setFileId(String fileId) {
			this.fileId = fileId;
		}
	}

}
