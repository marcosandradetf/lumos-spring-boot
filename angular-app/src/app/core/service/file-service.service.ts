import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import JSZip from 'jszip';
import {environment} from '../../../environments/environment';
import {firstValueFrom  } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class FileService {
    private endpoint: string = environment.springboot + '/api/s3';

    constructor(private http: HttpClient) {
    }


    async sendFile(files: File[]): Promise<any> {
        if (!files || files.length === 0) return null;

        const formData = new FormData();

        if (files.length === 1) {
            // apenas um arquivo, envia direto
            formData.append('file', files[0]);
        } else {
            // mais de um arquivo, cria ZIP
            const zip = new JSZip();
            for (const file of files) {
                zip.file(file.name, file);
            }

            const zipBlob = await zip.generateAsync({
                type: 'blob',
                compression: 'DEFLATE',
                compressionOptions: {level: 6}
            });
            formData.append('file', zipBlob, 'contract.zip');
        }

        return firstValueFrom(this.http.post(this.endpoint + '/upload-file', formData));
    }


    downloadFile(fileName: string) {
        return this.http.get(this.endpoint + `/download/${fileName}`, {
            responseType: 'blob',
            observe: 'response'
        });
    }

}
