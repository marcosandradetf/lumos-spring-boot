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

    return this.http.post(this.endpoint + '/upload', formData);
  }

}
