import {ComponentFixture, TestBed} from '@angular/core/testing';
import {StockistsComponent} from './stockists.component';

describe('StockistsComponent', () => {
    let component: StockistsComponent;
    let fixture: ComponentFixture<StockistsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [StockistsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(StockistsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
