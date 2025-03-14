import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RequestItemsComponent } from './request-items.component';

describe('RequestItemsComponent', () => {
  let component: RequestItemsComponent;
  let fixture: ComponentFixture<RequestItemsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RequestItemsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RequestItemsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
