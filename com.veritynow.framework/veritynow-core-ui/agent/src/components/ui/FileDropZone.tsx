import React, { useCallback, useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { Box, Typography, Paper, List, ListItem, ListItemText, IconButton } from '@mui/material';
import { CloudUpload as CloudUploadIcon, Close as CloseIcon } from '@mui/icons-material';

interface FileDropzoneProps {
  // You can add props here if your component needs them
}

const FileDropzone: React.FC<FileDropzoneProps> = () => {
  // Define the type of the files state as an array of File objects
  const [files, setFiles] = useState<File[]>([]);

  // The 'onDrop' function takes an array of File objects
  const onDrop = useCallback((acceptedFiles: File[]) => {
    setFiles(prevFiles => {
      const existingFileNames = new Set(prevFiles.map(f => f.name));
      const newFiles = acceptedFiles.filter(f => !existingFileNames.has(f.name));
      return [...prevFiles, ...newFiles];
    });
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({ onDrop });

  const removeFile = (fileName: string) => {
    setFiles(prevFiles => prevFiles.filter(file => file.name !== fileName));
  };

  return (
    <Box>
      <Paper
        variant="outlined"	  sx={{
	            padding: 4,
	            textAlign: 'center',
	            cursor: 'pointer',
	            borderStyle: 'dashed',
	            borderColor: isDragActive ? 'primary.main' : 'grey.400',
	            backgroundColor: isDragActive ? 'grey.50' : 'background.paper',
	            transition: 'background-color 0.3s',
	          }}
	          {...getRootProps()}
	        >
	          <input {...getInputProps()} />
	          <CloudUploadIcon color="action" sx={{ fontSize: 60 }} />
	          <Typography variant="body1" sx={{ mt: 2 }}>
	            {isDragActive ? 'Drop the files here...' : 'Drag and drop files here, or click to select files'}
	          </Typography>
	        </Paper>

	        {files.length > 0 && (
	          <Box sx={{ mt: 3 }}>
	            <Typography variant="h6">Uploaded Files:</Typography>
	            <List>
	              {files.map(file => (
	                <ListItem
	                  key={file.name}
	                  secondaryAction={
	                    <IconButton edge="end" aria-label="delete" onClick={() => removeFile(file.name)}>
	                      <CloseIcon />
	                    </IconButton>
	                  }
	                >
	                  <ListItemText
	                    primary={file.name}
	                    secondary={`${(file.size / 1024).toFixed(2)} KB`}
	                  />
	                </ListItem>
	              ))}
	            </List>
	          </Box>
	        )}
	      </Box>);
	};
	
	export default FileDropzone;