import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MaterialCreateComponent } from './material-create.component';

describe('MaterialCreateComponent', () => {
  let component: MaterialCreateComponent;
  let fixture: ComponentFixture<MaterialCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MaterialCreateComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MaterialCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
