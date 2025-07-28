import {Component} from '@angular/core';
import {Menubar} from 'primeng/menubar';

@Component({
  selector: 'app-app-download',
  standalone: true,
  imports: [
    Menubar,
  ],
  templateUrl: './app-download.component.html',
  styleUrl: './app-download.component.scss'
})
export class AppDownloadComponent {

// no seu componente .ts
  downloadApk() {
    const link = document.createElement('a');
    link.href = 'https://api.thryon.com.br/minio/apk/com.thryon.apps.android.release_9_2.1.6.apk'; // link do arquivo que quer baixar
    link.download = 'com.thryon.apps.android.release_9_2.1.6.apk'; // nome que o arquivo terá ao baixar
    link.click();
  }

}
