import {Component} from '@angular/core';
import {Avatar} from 'primeng/avatar';
import {Badge} from 'primeng/badge';
import {Menu} from 'primeng/menu';
import {Menubar} from 'primeng/menubar';
import {NgForOf, NgIf} from '@angular/common';
import {Ripple} from 'primeng/ripple';

@Component({
  selector: 'app-app-download',
  standalone: true,
  imports: [
    Avatar,
    Badge,
    Menu,
    Menubar,
    NgForOf,
    NgIf,
    Ripple
  ],
  templateUrl: './app-download.component.html',
  styleUrl: './app-download.component.scss'
})
export class AppDownloadComponent {

// no seu componente .ts
  downloadApk() {
    const link = document.createElement('a');
    link.href = 'https://minio.thryon.com.br/apk/com.thryon.lumos_v2.apk'; // link do arquivo que quer baixar
    link.download = 'com.thryon.lumos_v2.apk'; // nome que o arquivo terá ao baixar
    link.click();
  }

}
