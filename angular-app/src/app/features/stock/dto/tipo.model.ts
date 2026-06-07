import {Group} from './grupo.model';

export class Type {
  idType: number;
  typeName: string;
  group: Group;

  constructor() {
    this.idType = 0;
    this.typeName = "";
    this.group = new Group();
  }
}
