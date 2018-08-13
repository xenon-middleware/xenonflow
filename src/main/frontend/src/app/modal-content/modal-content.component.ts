import { Component, OnInit, Input, ViewEncapsulation } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-modal-content',
  templateUrl: './modal-content.component.html',
  encapsulation: ViewEncapsulation.None,
  styleUrls: ['./modal-content.component.css']
})
export class ModalContentComponent implements OnInit {

  @Input() content;
  @Input() title;

  constructor(public activeModal: NgbActiveModal) {
    this.title='Modal Dialog';
    this.content='';
  }

  ngOnInit() {
  }

}
