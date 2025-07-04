import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ufRequest} from '../uf-request.dto';
import {citiesRequest} from '../cities-request.dto';
import {environment} from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private endpoint: string = environment.springboot + '/api/minio';

  constructor(private http: HttpClient) {
  }

  sendFile(file: File) {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post(this.endpoint + '/upload-file', formData);
  }

  sendFiles(files: File[]) {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));

    return this.http.post<string[]>(this.endpoint + '/upload-files', formData);
  }

  downloadFile(fileName: string) {
    return this.http.get(this.endpoint + `/download/${fileName}`, {
      responseType: 'blob',
      observe: 'response'
    });
  }

}
