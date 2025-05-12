import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-icon-error',
  standalone: true,
  imports: [],
  template: `
    <svg xmlns="http://www.w3.org/2000/svg" [attr.height]="size" viewBox="0 -960 960 960" [attr.width]="size" [attr.fill]="color"><path d="M480-280q17 0 28.5-11.5T520-320q0-17-11.5-28.5T480-360q-17 0-28.5 11.5T440-320q0 17 11.5 28.5T480-280Zm-40-160h80v-240h-80v240Zm40 360q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z"/></svg>
  `
})
export class IconErrorComponent {
  @Input() color: string = '#EA3323';
  @Input() size: string = '24px';
}

@Component({
  selector: 'app-icon-arrow-drop-down',
  standalone: true,
  template: `
    <svg xmlns="http://www.w3.org/2000/svg" [attr.height]="size" viewBox="0 -960 960 960" [attr.width]="size" [attr.fill]="color"><path d="M480-360 280-560h400L480-360Z"/></svg>
  `
})
export class IconArrowDropDownComponent {
  @Input() color: string = '#000000';
  @Input() size: string = '24px';
}

@Component({
  selector: 'app-icon-alert',
  standalone: true,
  template: `
    <svg xmlns="http://www.w3.org/2000/svg" [attr.height]="size" viewBox="0 -960 960 960" [attr.width]="size" [attr.fill]="color"><path d="m344-60-76-128-144-32 14-148-98-112 98-112-14-148 144-32 76-128 136 58 136-58 76 128 144 32-14 148 98 112-98 112 14 148-144 32-76 128-136-58-136 58Zm34-102 102-44 104 44 56-96 110-26-10-112 74-84-74-86 10-112-110-24-58-96-102 44-104-44-56 96-110 24 10 112-74 86 74 84-10 114 110 24 58 96Zm102-318Zm0 200q17 0 28.5-11.5T520-320q0-17-11.5-28.5T480-360q-17 0-28.5 11.5T440-320q0 17 11.5 28.5T480-280Zm-40-160h80v-240h-80v240Z"/></svg>
  `
})
export class IconAAlertComponent {
  @Input() color: string = '#EA3323';
  @Input() size: string = '24px';
}

