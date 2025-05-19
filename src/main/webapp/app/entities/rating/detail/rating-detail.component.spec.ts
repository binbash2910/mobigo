import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { RatingDetailComponent } from './rating-detail.component';

describe('Rating Management Detail Component', () => {
  let comp: RatingDetailComponent;
  let fixture: ComponentFixture<RatingDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatingDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./rating-detail.component').then(m => m.RatingDetailComponent),
              resolve: { rating: () => of({ id: 11381 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(RatingDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RatingDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('should load rating on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', RatingDetailComponent);

      // THEN
      expect(instance.rating()).toEqual(expect.objectContaining({ id: 11381 }));
    });
  });

  describe('PreviousState', () => {
    it('should navigate to previous state', () => {
      jest.spyOn(window.history, 'back');
      comp.previousState();
      expect(window.history.back).toHaveBeenCalled();
    });
  });
});
