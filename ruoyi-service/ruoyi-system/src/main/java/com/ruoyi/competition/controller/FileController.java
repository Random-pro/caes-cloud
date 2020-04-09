package com.ruoyi.competition.controller;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.utils.FileInfo;
import com.ruoyi.utils.FileControl;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.competition.domain.UploadList;
import com.ruoyi.competition.service.UploadListService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Date;


/**
 * 上传文件列表 提供者
 * 
 * @author Random-pro
 * @date 2020-02-14
 */
@RestController
@RequestMapping("file")
public class FileController extends BaseController
{

	@Autowired
	private UploadListService uploadListService;

	/**
	 *  文件上传下载 控制权限:0=禁止访问 1=需要授权(登录) 2=游客访问
	 *  POST: fileList/upload?upload=FILE
	 *  返回R:状态码
	 */
	@PostMapping("upload")
	public R upload(MultipartFile file) throws IOException
	{
		// 上传
		FileInfo fileInfo = FileControl.upload(file);
		// 检查上传状态
		switch(fileInfo.getSaveStatus())
		{
			case SUCCESS:{
				UploadList newFile = new UploadList();
				newFile.setFileId(fileInfo.getFileId());
				newFile.setFileName(fileInfo.getFileName());
				//newFile.setFileMd5(fileInfo.getFileMD5());
				newFile.setFileMd5("-");
				newFile.setFileSize(fileInfo.getFileSize());
				newFile.setFileAuth("2");
				newFile.setFilePath(fileInfo.getFileDir().toString());
				newFile.setUploadTime(new Date());
				uploadListService.insertUploadList(newFile);
				// 返回文件信息
				return R.ok().put("fileInfo", fileInfo);
			}
			default:return R.error().put("fileInfo", fileInfo);
		}
	}

	@GetMapping("download")
	public ResponseEntity<byte[]> download(HttpServletRequest request, String fileId) throws IOException {
		UploadList fileInfo = uploadListService.selectUploadListById(fileId);
		// 校验文件权限
		// switch(fileInfo.getFileAuth()) {
			File file = FileControl.download(fileInfo.getFilePath()).getFileDir();

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.setContentDispositionFormData("attachment", FileControl.fileNameEncode(fileInfo.getFileName(), request));

			// 增加下载计数
			fileInfo.setDownloadCount(fileInfo.getDownloadCount() + 1);
			uploadListService.updateUploadList(fileInfo);
			return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(file), headers, HttpStatus.OK);
		// }
	}

	/**
	 * 按fileId查询文件信息 ${tableComment}
	 */
	@GetMapping("get/{fileId}")
	public UploadList get(@PathVariable("fileId") String fileId)
	{
		return uploadListService.selectUploadListById(fileId);
	}
	
	/**
	 * 查询上传文件列表
	 */
	@GetMapping("list")
	public R list(UploadList uploadList)
	{
		startPage();
        return result(uploadListService.selectUploadList(uploadList));
	}

	/**
	 * 修改保存上传文件列表
	 */
	@PostMapping("update")
	public R editSave(@RequestBody UploadList uploadList)
	{		
		return toAjax(uploadListService.updateUploadList(uploadList));
	}
	
	/**
	 * 删除文件${tableComment}
	 */
	@PostMapping("delete")
	public R remove(@RequestBody String fileId)
	{
		UploadList fileInfo = uploadListService.selectUploadListById(fileId);

		// 删除文件记录
		uploadListService.deleteUploadListById(fileId);
		return FileControl.delete(fileInfo.getFilePath()) ? R.ok() : R.error();
	}
	
}
