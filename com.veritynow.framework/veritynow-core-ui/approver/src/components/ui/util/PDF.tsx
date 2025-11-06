import  {pdfjs}  from "react-pdf";
import worker from "pdfjs-dist/build/pdf.worker.min.mjs?url";
import dummy from "pdfjs-dist/build/pdf.worker.min.mjs?url"

if (!dummy) console.log("GOT NULL");
pdfjs.GlobalWorkerOptions.workerSrc = worker.toString();


async function ensurePdfJs() {
  pdfjs.GlobalWorkerOptions.workerSrc = worker.toString();
  return pdfjs;
}

export const PDF = {

 async getPagesAsImage(pdfUrl: string, pageNumber: number = 0, scale: number = 1.0): Promise<string[]> {
  const pdfjs = await ensurePdfJs();
  
  const pages = [];
  const loadingTask = await pdfjs.getDocument(pdfUrl);
  const pdf = await loadingTask.promise;
  const numPages = pdf.numPages;
  
  //If pageNumber is positive it means the caller is requesting for a specific page
  
  for (let i = 1; i <= numPages; i++) {
      
    //A user is requesting for a specific pageNumber
    if (pageNumber > 0) {
          if (i > pageNumber) 
            //we are done with the required page   
            break;
          else
            //go to the specified page
            i = pageNumber;
    }

    const page = await pdf.getPage(i);
    const viewport = page.getViewport({ scale: scale });

    // Prepare canvas
    const canvas = document.createElement('canvas');
    const canvasContext = canvas.getContext('2d')!;
    canvas.height = viewport.height;
    canvas.width = viewport.width;

    await page.render({ canvasContext, canvas, viewport }).promise;

  
    // Convert canvas to a data URL for use in an <img> tag
    pages.push(canvas.toDataURL('image/png',300));

    try {
    page.cleanup?.();
    pdf.cleanup?.();
    loadingTask.destroy?.();
  } catch {/** do nothing */} 

  }
  return pages;
},

 async thumbnail(pdfUrl: string, pageNumber: number = 1, scale: number = 1.0): Promise<string> {
  return (await this.getPagesAsImage(pdfUrl, pageNumber,scale))[0];
},


}

export default PDF;






